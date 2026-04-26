package com.nadila.customer_management_api.service.bulkCustomerService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nadila.customer_management_api.dto.responseDto.BulkErrorEntryDto;
import com.nadila.customer_management_api.dto.responseDto.BulkJobResponseDto;
import com.nadila.customer_management_api.dto.responseDto.BulkJobResponseDto.ErrorSummary;
import com.nadila.customer_management_api.entity.*;
import com.nadila.customer_management_api.enums.BulkJobStatus;
import com.nadila.customer_management_api.exception.*;
import com.nadila.customer_management_api.repository.BulkJobRepository;
import com.nadila.customer_management_api.repository.CustomerRepository;
import com.nadila.customer_management_api.service.cityService.CityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkCustomerServiceImpl implements BulkCustomerService {

    private final CustomerRepository customerRepository;
    private final BulkJobRepository  bulkJobRepository;
    private final CityService        cityService;
    private final ObjectMapper       objectMapper;

    private static final int BATCH_SIZE = 500;
    private static final int MAX_ERRORS = 100;


    @Override
    public BulkJobResponseDto initiateBulkUpload(MultipartFile file) {

        if (file.isEmpty())
            throw new InvalidRequestException("File is empty");
        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".xlsx"))
            throw new InvalidRequestException("Only .xlsx files are supported");

        String jobId = UUID.randomUUID().toString();
        BulkJob job = BulkJob.builder()
                .jobId(jobId)
                .status(BulkJobStatus.PENDING)
                .totalRecords(0)
                .processedRecords(0)
                .failedRecords(0)
                .insertedCount(0)
                .updatedCount(0)
                .build();
        bulkJobRepository.save(job);

        try {
            Path tempFile = Files.createTempFile("bulk_", ".xlsx");
            file.transferTo(tempFile);
            processAsync(jobId, tempFile);
        } catch (IOException e) {
            job.setStatus(BulkJobStatus.FAILED);
            job.setErrorDetails("Failed to read file: " + e.getMessage());
            bulkJobRepository.save(job);
        }

        return toResponseDto(job);
    }


    @Async("bulkExecutor")
    public void processAsync(String jobId, Path tempFile) {

        BulkJob job = bulkJobRepository.findByJobId(jobId).orElseThrow();
        job.setStatus(BulkJobStatus.PROCESSING);
        bulkJobRepository.save(job);

        List<BulkErrorEntryDto> errors      = new ArrayList<>();
        List<Customer>       batchCreate = new ArrayList<>();
        List<Customer>       batchUpdate = new ArrayList<>();

        Set<String> seenNics    = new HashSet<>();
        int totalRecords  = 0;
        int failedRecords = 0;
        int insertedCount = 0;
        int updatedCount  = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(tempFile.toFile())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                totalRecords++;

                try {
                    UpsertResult result = parseAndUpsertRow(row, seenNics);

                    if (result.isNew()) {
                        batchCreate.add(result.customer());
                    } else {
                        batchUpdate.add(result.customer());
                    }


                    if (batchCreate.size() == BATCH_SIZE) {
                        saveBatch(batchCreate);
                        insertedCount += BATCH_SIZE;
                        job.setInsertedCount(insertedCount);
                        job.setProcessedRecords(insertedCount + updatedCount);
                        bulkJobRepository.save(job);
                        batchCreate.clear();
                        log.info("Job {} -- create batch flushed, inserted: {}", jobId, insertedCount);
                    }


                    if (batchUpdate.size() == BATCH_SIZE) {
                        saveBatch(batchUpdate);
                        updatedCount += BATCH_SIZE;
                        job.setUpdatedCount(updatedCount);
                        job.setProcessedRecords(insertedCount + updatedCount);
                        bulkJobRepository.save(job);
                        batchUpdate.clear();
                        log.info("Job {} -- update batch flushed, updated: {}", jobId, updatedCount);
                    }

                } catch (Exception e) {
                    failedRecords++;
                    if (errors.size() < MAX_ERRORS) {
                        errors.add(new BulkErrorEntryDto(row.getRowNum() + 1, e.getMessage()));
                    }
                    log.warn("Row {} failed: {}", row.getRowNum() + 1, e.getMessage());
                }
            }


            if (!batchCreate.isEmpty()) {
                saveBatch(batchCreate);
                insertedCount += batchCreate.size();
            }
            if (!batchUpdate.isEmpty()) {
                saveBatch(batchUpdate);
                updatedCount += batchUpdate.size();
            }


            job.setTotalRecords(totalRecords);
            job.setInsertedCount(insertedCount);
            job.setUpdatedCount(updatedCount);
            job.setProcessedRecords(insertedCount + updatedCount);
            job.setFailedRecords(failedRecords);
            job.setStatus(failedRecords > 0
                    ? BulkJobStatus.COMPLETED_WITH_ERRORS
                    : BulkJobStatus.COMPLETED);


            if (!errors.isEmpty()) {
                try {
                    job.setErrorDetails(objectMapper.writeValueAsString(errors));
                } catch (Exception ignored) {
                    job.setErrorDetails("Error serialization failed");
                }
            }

            log.info("Job {} complete -- total: {}, inserted: {}, updated: {}, failed: {}",
                    jobId, totalRecords, insertedCount, updatedCount, failedRecords);

        } catch (Exception e) {
            job.setStatus(BulkJobStatus.FAILED);
            job.setErrorDetails("Processing failed: " + e.getMessage());
            log.error("Job {} failed: {}", jobId, e.getMessage());

        } finally {
            bulkJobRepository.save(job);
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
        }
    }


    private UpsertResult parseAndUpsertRow(Row row, Set<String> seenNics) {


        String name             = getCellValue(row, 0);
        String dob              = getCellValue(row, 1);
        String nicNumber        = getCellValue(row, 2);
        String phonesRaw        = getCellValue(row, 3);
        String addressLine1sRaw = getCellValue(row, 4);
        String addressLine2sRaw = getCellValue(row, 5);
        String cityNamesRaw     = getCellValue(row, 6);
        String countriesRaw     = getCellValue(row, 7);
        String familyMembersRaw = getCellValue(row, 8);


        if (name == null || name.isBlank())
            throw new InvalidRequestException("Name is required");
        if (dob == null || dob.isBlank())
            throw new InvalidRequestException("Date of birth is required");
        if (nicNumber == null || nicNumber.isBlank())
            throw new InvalidRequestException("NIC is required");


        if (!seenNics.add(nicNumber)) {
            throw new DuplicateResourceException(
                    "Duplicate NIC in file: " + nicNumber +
                            " -- this NIC already appeared in an earlier row");
        }

        List<String> phoneList = Collections.emptyList();
        if (phonesRaw != null && !phonesRaw.isBlank()) {
            phoneList = Arrays.stream(phonesRaw.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isBlank())
                    .toList();
        }


        List<String> line1List   = Collections.emptyList();
        List<String> line2List   = Collections.emptyList();
        List<String> cityList    = Collections.emptyList();
        List<String> countryList = Collections.emptyList();

        boolean hasAddresses = addressLine1sRaw != null && !addressLine1sRaw.isBlank();
        if (hasAddresses) {

            line1List = Arrays.stream(addressLine1sRaw.split("\\|"))
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .toList();

            if (cityNamesRaw == null || cityNamesRaw.isBlank())
                throw new InvalidRequestException(
                        "cityNames (column G) is required when addressLine1s is provided");

            cityList = Arrays.stream(cityNamesRaw.split("\\|"))
                    .map(String::trim)
                    .toList();

            if (line1List.size() != cityList.size())
                throw new InvalidRequestException(
                        "addressLine1s count (" + line1List.size() +
                                ") must match cityNames count (" + cityList.size() + ")");

            if (addressLine2sRaw != null && !addressLine2sRaw.isBlank()) {
                line2List = Arrays.stream(addressLine2sRaw.split("\\|"))
                        .map(String::trim)
                        .toList();
            }

            if (countriesRaw != null && !countriesRaw.isBlank()) {
                countryList = Arrays.stream(countriesRaw.split("\\|"))
                        .map(String::trim)
                        .toList();
            }
        }


        List<String> familyNicList = Collections.emptyList();
        if (familyMembersRaw != null && !familyMembersRaw.isBlank()) {

            List<String> pairs = Arrays.stream(familyMembersRaw.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isBlank())
                    .toList();

            familyNicList = new ArrayList<>();
            for (String pair : pairs) {
                String[] parts = pair.split("\\|");

                if (parts.length != 2 || parts[1].isBlank())
                    throw new InvalidRequestException(
                            "Invalid family member format: '" + pair +
                                    "'. Expected: name|nic  e.g. Kasun Silva|NIC002LK");

                familyNicList.add(parts[1].trim());
            }

            if (familyNicList.stream().distinct().count() != familyNicList.size())
                throw new DuplicateFamilyMemberException(
                        "Duplicate family member NICs for customer: " + nicNumber);

            if (familyNicList.contains(nicNumber))
                throw new SelfFamilyReferenceException(
                        "Customer cannot be their own family member: " + nicNumber);
        }


        Optional<Object> existing = customerRepository.findByNicNumber(nicNumber);
        boolean isNew = existing.isEmpty();
        Customer customer = isNew ? new Customer() : (Customer) existing.get();

        customer.setName(name);
        customer.setDateOfBirth(LocalDate.parse(dob));
        customer.setNicNumber(nicNumber);


        if (customer.getPhones() == null) {
            customer.setPhones(new ArrayList<>());
        } else {
            customer.getPhones().clear();
        }
        for (String phone : phoneList) {
            customer.getPhones().add(
                    CustomerPhone.builder()
                            .customer(customer)
                            .mobileNumber(phone)
                            .build());
        }


        if (customer.getAddresses() == null) {
            customer.setAddresses(new ArrayList<>());
        } else {
            customer.getAddresses().clear();
        }
        for (int i = 0; i < line1List.size(); i++) {

            String line2 = (line2List.size() > i
                    && !line2List.get(i).equalsIgnoreCase("null")
                    && !line2List.get(i).isBlank())
                    ? line2List.get(i) : null;

            City city = cityService.getCityByName(cityList.get(i));

            customer.getAddresses().add(
                    CustomerAddress.builder()
                            .customer(customer)
                            .addressLine1(line1List.get(i))
                            .addressLine2(line2)
                            .city(city)
                            .build());
        }


        if (customer.getFamilyLinks() == null) {
            customer.setFamilyLinks(new ArrayList<>());
        } else {
            customer.getFamilyLinks().clear();
        }
        for (String familyNic : familyNicList) {
            Customer familyMember = (Customer) customerRepository.findByNicNumber(familyNic)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Family member NIC not found: " + familyNic));

            customer.getFamilyLinks().add(
                    CustomerFamily.builder()
                            .customer(customer)
                            .familyMember(familyMember)
                            .build());
        }

        return new UpsertResult(customer, isNew);
    }


    @Transactional
    public void saveBatch(List<Customer> batch) {
        customerRepository.saveAll(batch);
    }


    @Override
    public BulkJobResponseDto getJobStatus(String jobId) {
        BulkJob job = bulkJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with ID: " + jobId));
        return toResponseDto(job);
    }


    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return DateUtil.isCellDateFormatted(cell)
                        ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                        : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
    private BulkJobResponseDto toResponseDto(BulkJob job) {

        ErrorSummary errorSummary = null;

        if (job.getErrorDetails() != null && !job.getErrorDetails().isBlank()) {
            List<BulkErrorEntryDto> items = new ArrayList<>();
            try {
                items = objectMapper.readValue(
                        job.getErrorDetails(),
                        new TypeReference<List<BulkErrorEntryDto>>() {});
            } catch (Exception e) {

                items.add(new BulkErrorEntryDto(0, job.getErrorDetails()));
            }

            String note = items.size() >= MAX_ERRORS
                    ? "showing first " + MAX_ERRORS + " errors"
                    : null;

            errorSummary = ErrorSummary.builder()
                    .totalErrors(job.getFailedRecords())
                    .note(note)
                    .items(items)
                    .build();
        }

        return BulkJobResponseDto.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .insertedCount(job.getInsertedCount())
                .updatedCount(job.getUpdatedCount())
                .failedRecords(job.getFailedRecords())
                .errors(errorSummary)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private record UpsertResult(Customer customer, boolean isNew) {}
}