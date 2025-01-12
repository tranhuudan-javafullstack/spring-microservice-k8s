package com.huudan.accounts.service.impl;

import com.huudan.accounts.dto.AccountsDto;
import com.huudan.accounts.dto.CardsDto;
import com.huudan.accounts.dto.CustomerDetailsDto;
import com.huudan.accounts.dto.LoansDto;
import com.huudan.accounts.entity.Accounts;
import com.huudan.accounts.entity.Customer;
import com.huudan.accounts.exception.ResourceNotFoundException;
import com.huudan.accounts.mapper.AccountsMapper;
import com.huudan.accounts.mapper.CustomerMapper;
import com.huudan.accounts.repository.AccountsRepository;
import com.huudan.accounts.repository.CustomerRepository;
import com.huudan.accounts.service.ICustomersService;
import com.huudan.accounts.service.client.CardsFeignClient;
import com.huudan.accounts.service.client.LoansFeignClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class CustomersServiceImpl implements ICustomersService {

    AccountsRepository accountsRepository;
    CustomerRepository customerRepository;
    CardsFeignClient cardsFeignClient;
    LoansFeignClient loansFeignClient;

    /**
     * @param mobileNumber  - Input Mobile Number
     * @param correlationId - Correlation ID value generated at Edge server
     * @return Customer Details based on a given mobileNumber
     */
    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(() -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString()));

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);
        if (null != loansDtoResponseEntity) {
            customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());
        }

        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);
        if (null != cardsDtoResponseEntity) {
            customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());
        }


        return customerDetailsDto;

    }
}
