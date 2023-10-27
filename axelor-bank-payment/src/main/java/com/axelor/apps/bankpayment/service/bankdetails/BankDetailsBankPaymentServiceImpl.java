package com.axelor.apps.bankpayment.service.bankdetails;

import com.axelor.apps.bankpayment.db.BankStatementLineAFB120;
import com.axelor.apps.bankpayment.service.bankstatementline.afb120.BankStatementLineAFB120Service;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.repo.BankDetailsRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

public class BankDetailsBankPaymentServiceImpl implements BankDetailsBankPaymentService {
  protected BankStatementLineAFB120Service bankStatementLineAFB120Service;
  protected BankDetailsRepository bankDetailsRepository;

  @Inject
  public BankDetailsBankPaymentServiceImpl(
      BankStatementLineAFB120Service bankStatementLineAFB120Service,
      BankDetailsRepository bankDetailsRepository) {
    this.bankStatementLineAFB120Service = bankStatementLineAFB120Service;
    this.bankDetailsRepository = bankDetailsRepository;
  }

  @Override
  @Transactional
  public void updateBankDetailsBalanceAndDate(List<BankDetails> bankDetails) {
    if (CollectionUtils.isEmpty(bankDetails)) {
      return;
    }
    BankStatementLineAFB120 lastLine;
    for (BankDetails bankDetail : bankDetails) {
      lastLine =
          bankStatementLineAFB120Service.getLastBankStatementLineAFB120FromBankDetails(bankDetail);
      if (lastLine != null) {
        bankDetail.setBalance(
            lastLine.getDebit().compareTo(BigDecimal.ZERO) > 0
                ? lastLine.getDebit()
                : lastLine.getCredit());
        bankDetail.setBalanceUpdatedDate(lastLine.getOperationDate());
      } else {
        bankDetail.setBalance(BigDecimal.ZERO);
        bankDetail.setBalanceUpdatedDate(null);
      }
      bankDetailsRepository.save(bankDetail);
    }
  }
}