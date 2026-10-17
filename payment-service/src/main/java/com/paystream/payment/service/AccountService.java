package com.paystream.payment.service;

import com.paystream.payment.repository.AccountRepository;
import com.paystream.payment.web.dto.AccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read access to account reference data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts() {
        List<AccountResponse> accounts = accountRepository.findAll(Sort.by("accountNumber")).stream()
                .map(AccountResponse::from)
                .toList();
        log.debug("Listed {} accounts", accounts.size());
        return accounts;
    }
}
