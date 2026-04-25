package com.nadila.customer_management_api.service.bulkCustomerService;

import com.nadila.customer_management_api.dto.responseDto.BulkJobResponseDto;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkCustomerServiceImpl implements BulkCustomerService {

    private final CustomerRepository customerRepository;
    private final BulkJobRepository bulkJobRepository;
    private final CityService cityService;

    private static final int BATCH_SIZE = 500;

    // =================================================================
    // 1. INITIATE UPLOAD  -- returns jobId immediately (non-blocking)
    // =================================================================
    @Override
    public BulkJobResponseDto initiateBulkUpload(MultipartFile file) {

        if (file.isEmpty()) {
            throw new InvalidRequestException("File is empty");
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".xlsx")) {
            throw new InvalidRequestException("Only .xlsx files are supported");
        }

        String jobId = UUID.randomUUID().toString();
        BulkJob job  = BulkJob.builder()
                .jobId(jobId)
                .status(BulkJobStatus.PENDING)
                .totalRecords(0)
                .processedRecords(0)
                .failedRecords(0)
                .build();
        bulkJobRepository.save(job);

        try {
            // Copy to temp file -- frees HTTP thread memory immediately
            Path tempFile = Files.createTempFile("bulk_", ".xlsx");
            file.transferTo(tempFile);
            processAsync(jobId, tempFile);   // non-blocking

        } catch (IOException e) {
            job.setStatus(BulkJobStatus.FAILED);
            job.setErrorDetails("Failed to read file: " + e.getMessage());
            bulkJobRepository.save(job);
        }

        return toResponseDto(job);
    }

    // =================================================================
    // 2. ASYNC PROCESSING  -- runs in bulkExecutor thread pool
    // =================================================================
    @Async("bulkExecutor")
    public void processAsync(String jobId, Path tempFile) {

        BulkJob job = bulkJobRepository.findByJobId(jobId).orElseThrow();
        job.setStatus(BulkJobStatus.PROCESSING);
        bulkJobRepository.save(job);

        List<String>   errors      = new ArrayList<>();
        List<Customer> batchCreate = new ArrayList<>();
        List<Customer> batchUpdate = new ArrayList<>();

        /*
         * ROOT CAUSE FIX for Duplicate entry constraint violation:
         *
         * The upsert check (findByNicNumber) happens BEFORE saveBatch().
         * If the same NIC appears twice in the Excel file, both rows pass
         * the DB check (neither is in the DB yet when checked), both get
         * added to the batch, and the batch insert then hits the unique
         * constraint and fails the ENTIRE job.
         *
         * Fix: maintain a seenNics Set in memory for this file.
         * Any NIC already seen in a previous row is caught here, before
         * it can reach the batch, and that row is failed individually
         * without affecting the rest.
         */
        Set<String> seenNics  = new HashSet<>();
        int totalRecords  = 0;
        int failedRecords = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(tempFile.toFile())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;   // skip header row
                totalRecords++;

                try {
                    // seenNics is passed in so parseAndUpsertRow can both
                    // check and register the NIC atomically for this file
                    UpsertResult result = parseAndUpsertRow(row, seenNics);

                    if (result.isNew()) {
                        batchCreate.add(result.customer());
                    } else {
                        batchUpdate.add(result.customer());
                    }

                    // Flush create batch
                    if (batchCreate.size() == BATCH_SIZE) {
                        saveBatch(batchCreate);
                        job.setProcessedRecords(job.getProcessedRecords() + BATCH_SIZE);
                        bulkJobRepository.save(job);
                        batchCreate.clear();
                        log.info("Job {} -- create batch flushed, processed: {}",
                                jobId, job.getProcessedRecords());
                    }

                    // Flush update batch
                    if (batchUpdate.size() == BATCH_SIZE) {
                        saveBatch(batchUpdate);
                        job.setProcessedRecords(job.getProcessedRecords() + BATCH_SIZE);
                        bulkJobRepository.save(job);
                        batchUpdate.clear();
                        log.info("Job {} -- update batch flushed, processed: {}",
                                jobId, job.getProcessedRecords());
                    }

                } catch (Exception e) {
                    failedRecords++;
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    log.warn("Row {} failed: {}", row.getRowNum() + 1, e.getMessage());
                }
            }

            // Save remaining records in last batches
            if (!batchCreate.isEmpty()) {
                saveBatch(batchCreate);
                job.setProcessedRecords(job.getProcessedRecords() + batchCreate.size());
            }
            if (!batchUpdate.isEmpty()) {
                saveBatch(batchUpdate);
                job.setProcessedRecords(job.getProcessedRecords() + batchUpdate.size());
            }

            // Final job status
            job.setTotalRecords(totalRecords);
            job.setFailedRecords(failedRecords);
            job.setStatus(failedRecords > 0
                    ? BulkJobStatus.COMPLETED_WITH_ERRORS
                    : BulkJobStatus.COMPLETED);

            if (!errors.isEmpty()) {
                job.setErrorDetails(errors.stream()
                        .limit(100)   // cap at first 100 error messages
                        .collect(Collectors.joining("\n")));
            }

            log.info("Job {} complete -- total: {}, failed: {}",
                    jobId, totalRecords, failedRecords);

        } catch (Exception e) {
            job.setStatus(BulkJobStatus.FAILED);
            job.setErrorDetails("Processing failed: " + e.getMessage());
            log.error("Job {} failed: {}", jobId, e.getMessage());

        } finally {
            bulkJobRepository.save(job);
            try { Files.deleteIfExists(tempFile); }   // free disk space
            catch (IOException ignored) {}
        }
    }

    // =================================================================
    // 3. PARSE ROW  -- upsert by NIC (create if new, update if exists)
    // =================================================================

    /*
     * Excel column layout:
     *
     *  Col  Field            Required   Format / Notes
     *  ---  ---------------  --------   -----------------------------------------------
     *   A   name             YES
     *   B   dateOfBirth      YES        yyyy-MM-dd
     *   C   nicNumber        YES        unique upsert key
     *   D   phones           no         comma-separated    +94771234561,+94771234562
     *   E   addressLine1s    no         pipe-separated     123 Main St|45 Lake Rd
     *   F   addressLine2s    no         pipe-separated     Floor 2|null
     *   G   cityNames        no *       pipe-separated     Colombo|Kandy
     *   H   countries        no         pipe-separated     Sri Lanka|India
     *   I   familyMembers    no         comma-sep name|nic Kasun Silva|NIC002LK,Arjun|NIC003LK
     *
     *  * cityNames (G) is required when addressLine1s (E) is provided.
     *  * Use the literal word "null" as a pipe-column placeholder to keep index alignment.
     *  * Leave any optional column completely blank to skip it.
     *
     *  Example row:
     *  Nadila | 2000-03-15 | NIC001LK | +94771234561,+94771234562 | 123 Main|45 Lake |
     *  Floor 2|null | Colombo|Kandy | Sri Lanka|null | Kasun Silva|NIC002LK,Arjun|NIC003LK
     */
    private UpsertResult parseAndUpsertRow(Row row, Set<String> seenNics) {

        // -- Read raw cell values -------------------------------------
        String name             = getCellValue(row, 0);
        String dob              = getCellValue(row, 1);
        String nicNumber        = getCellValue(row, 2);
        String phonesRaw        = getCellValue(row, 3);   // optional
        String addressLine1sRaw = getCellValue(row, 4);   // optional
        String addressLine2sRaw = getCellValue(row, 5);   // optional
        String cityNamesRaw     = getCellValue(row, 6);   // required if E is provided
        String countriesRaw     = getCellValue(row, 7);   // optional
        String familyMembersRaw = getCellValue(row, 8);   // optional

        // -- Mandatory field validation --------------------------------
        if (name == null || name.isBlank())
            throw new InvalidRequestException("Name is required");
        if (dob == null || dob.isBlank())
            throw new InvalidRequestException("Date of birth is required");
        if (nicNumber == null || nicNumber.isBlank())
            throw new InvalidRequestException("NIC is required");

        /*
         * Duplicate NIC guard (in-file check):
         * Catches the case where the same NIC appears more than once
         * in the Excel file before either row has been flushed to DB.
         * Without this, both rows pass findByNicNumber (DB has neither),
         * both land in batchCreate, and the batch insert explodes.
         */
        if (!seenNics.add(nicNumber)) {
            throw new DuplicateResourceException(
                    "Duplicate NIC in file: " + nicNumber +
                            " -- this NIC already appeared in an earlier row");
        }

        // -- Parse phones (optional) ----------------------------------
        List<String> phoneList = Collections.emptyList();
        if (phonesRaw != null && !phonesRaw.isBlank()) {
            phoneList = Arrays.stream(phonesRaw.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isBlank())
                    .toList();
        }

        // -- Parse addresses (optional as a group) --------------------
        // If addressLine1s (E) is present, cityNames (G) must also be
        // present with the same pipe-count.
        // addressLine2s (F) and countries (H) are optional per address.
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

        // -- Parse family members (optional) --------------------------
        // Format per entry:  name|nic  e.g.  Kasun Silva|NIC002LK
        // name is display-only -- only the NIC drives the DB lookup.
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

                // parts[0] = name (ignored), parts[1] = NIC (used for lookup)
                familyNicList.add(parts[1].trim());
            }

            if (familyNicList.stream().distinct().count() != familyNicList.size())
                throw new DuplicateFamilyMemberException(
                        "Duplicate family member NICs for customer: " + nicNumber);

            if (familyNicList.contains(nicNumber))
                throw new SelfFamilyReferenceException(
                        "Customer cannot be their own family member: " + nicNumber);
        }

        // -- Upsert: find by NIC -> update if found, create if not ----
        Optional<Object> existing = customerRepository.findByNicNumber(nicNumber);
        boolean isNew = existing.isEmpty();
        Customer customer = isNew ? new Customer() : (Customer) existing.get();

        // Scalar fields -- always overwrite from Excel
        customer.setName(name);
        customer.setDateOfBirth(LocalDate.parse(dob));
        customer.setNicNumber(nicNumber);

        // -- Phones: REPLACE ALL --------------------------------------
        // orphanRemoval on the entity handles DB deletes for removed phones
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

        // -- Addresses: REPLACE ALL -----------------------------------
        if (customer.getAddresses() == null) {
            customer.setAddresses(new ArrayList<>());
        } else {
            customer.getAddresses().clear();
        }
        for (int i = 0; i < line1List.size(); i++) {

            // addressLine2 -- null if "null" placeholder or index out of range
            String line2 = (line2List.size() > i
                    && !line2List.get(i).equalsIgnoreCase("null")
                    && !line2List.get(i).isBlank())
                    ? line2List.get(i) : null;

            // country is resolved via city.getCountry() automatically in the entity.
            // If your CustomerAddress has its own separate Country field, uncomment:
            // String countryName = (countryList.size() > i
            //                       && !countryList.get(i).equalsIgnoreCase("null")
            //                       && !countryList.get(i).isBlank())
            //                      ? countryList.get(i) : null;

            City city = cityService.getCityByName(cityList.get(i)); // cached -- zero DB call

            customer.getAddresses().add(
                    CustomerAddress.builder()
                            .customer(customer)
                            .addressLine1(line1List.get(i))
                            .addressLine2(line2)
                            .city(city)
                            .build());
        }

        // -- Family members: REPLACE ALL ------------------------------
        if (customer.getFamilyLinks() == null) {
            customer.setFamilyLinks(new ArrayList<>());
        } else {
            customer.getFamilyLinks().clear();
        }
        for (String familyNic : familyNicList) {
            // Fail this row if the family member NIC does not exist yet
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

    // =================================================================
    // 4. BATCH SAVE
    // =================================================================
    @Transactional
    public void saveBatch(List<Customer> batch) {
        customerRepository.saveAll(batch);   // single batch insert / update
    }

    // =================================================================
    // 5. GET JOB STATUS
    // =================================================================
    @Override
    public BulkJobResponseDto getJobStatus(String jobId) {
        BulkJob job = bulkJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with ID: " + jobId));
        return toResponseDto(job);
    }

    // =================================================================
    // 6. HELPERS
    // =================================================================
    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private BulkJobResponseDto toResponseDto(BulkJob job) {
        return BulkJobResponseDto.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .processedRecords(job.getProcessedRecords())
                .failedRecords(job.getFailedRecords())
                .errorDetails(job.getErrorDetails())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    // =================================================================
    // 7. INNER RECORD -- carries upsert result back to the batch loop
    // =================================================================
    private record UpsertResult(Customer customer, boolean isNew) {}
}
