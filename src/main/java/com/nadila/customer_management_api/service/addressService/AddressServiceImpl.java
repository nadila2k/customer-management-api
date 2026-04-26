package com.nadila.customer_management_api.service.addressService;

import com.nadila.customer_management_api.exception.ResourceNotFoundException;
import com.nadila.customer_management_api.repository.CustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final CustomerAddressRepository customerAddressRepository;

    @Override
    public void deleteAddress(Long addressId) {
        customerAddressRepository.findById(addressId)
                .ifPresentOrElse(
                        customerAddressRepository::delete,
                        () -> { throw new ResourceNotFoundException(
                                "Address not found with ID: " + addressId); }
                );
    }
}
