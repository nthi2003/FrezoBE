package com.frezo.accounting.service;

import com.frezo.accounting.dto.response.BankStatementDto;
import com.frezo.accounting.dto.response.BankStatementLineDto;
import com.frezo.accounting.dto.response.MatchSuggestionDto;
import com.frezo.common.response.FePage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BankStatementService {

    List<BankStatementDto> list();

    BankStatementDto importCsv(String accountId, MultipartFile file);

    FePage<BankStatementLineDto> listLines(String statementId, String status);

    List<MatchSuggestionDto> suggestions(String statementId, String lineId, String mode);

    BankStatementLineDto match(String lineId, String journalEntryLineId);

    BankStatementLineDto unmatch(String lineId);

    BankStatementDto lock(String id);

    BankStatementDto reopen(String id);
}
