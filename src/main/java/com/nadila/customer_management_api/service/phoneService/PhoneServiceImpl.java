package com.nadila.customer_management_api.service.phoneService;

import com.nadila.customer_management_api.exception.ResourceNotFoundException;
import com.nadila.customer_management_api.repository.CustomerPhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PhoneServiceImpl implements PhoneService {

    private final CustomerPhoneRepository customerPhoneRepository;

    @Override
    public void deletePhone(Long phoneId) {
        customerPhoneRepository.findById(phoneId)
                .ifPresentOrElse(
                        phone -> {

                            long phoneCount = customerPhoneRepository
                                    .countByCustomerId(phone.getCustomer().getId());
                            customerPhoneRepository.delete(phone);
                        },
                        () -> { throw new ResourceNotFoundException(
                                "Phone not found with ID: " + phoneId); }
                );
    }
}
