package com.axelor.apps.contract.batch;

import com.axelor.apps.base.db.repo.BatchRepository;
import com.axelor.apps.base.service.administration.AbstractBatch;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.contract.db.ContractBatch;
import com.axelor.apps.contract.db.ContractVersion;
import com.axelor.apps.contract.db.repo.ContractBatchRepository;
import com.axelor.apps.contract.db.repo.ContractRepository;
import com.axelor.apps.contract.db.repo.ContractVersionRepository;
import com.axelor.apps.contract.service.ContractService;
import com.axelor.apps.contract.translation.ITranslation;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.i18n.I18n;
import com.axelor.message.db.Template;
import com.axelor.message.service.TemplateMessageService;
import com.axelor.meta.CallMethod;
import com.axelor.studio.db.AppContract;
import com.google.inject.Inject;
import java.time.LocalDate;
import java.util.List;

public class BatchContractReminderMail extends AbstractBatch {

  protected ContractRepository contractRepository;
  protected ContractVersionRepository contractVersionRepository;
  protected ContractService contractService;
  protected ContractBatchRepository contractBatchRepository;
  protected AppBaseService appBaseService;
  protected TemplateMessageService templateMessageService;

  @Inject
  public BatchContractReminderMail(
      ContractRepository contractRepository,
      ContractVersionRepository contractVersionRepository,
      ContractService contractService,
      ContractBatchRepository contractBatchRepository,
      AppBaseService appBaseService,
      TemplateMessageService templateMessageService) {
    this.contractRepository = contractRepository;
    this.contractVersionRepository = contractVersionRepository;
    this.contractService = contractService;
    this.contractBatchRepository = contractBatchRepository;
    this.appBaseService = appBaseService;
    this.templateMessageService = templateMessageService;
  }

  @CallMethod
  public LocalDate getEndOfPeriod() {
    AppContract appContract = (AppContract) appBaseService.getApp("contract");
    Integer duration = appContract.getDuration();
    Integer periodType = appContract.getPeriodTypeSelect();
    LocalDate date = LocalDate.now();

    switch (periodType) {
      case ContractBatchRepository.DAYS_PERIOD:
        date = date.plusDays(duration);
        break;

      case ContractBatchRepository.WEEKS_PERIOD:
        date = date.plusWeeks(duration);
        break;

      case ContractBatchRepository.MONTHS_PERIOD:
        date = date.plusMonths(duration);
        break;
    }

    return date;
  }

  protected LocalDate getEndOfPeriod(ContractBatch contractBatch) {
    LocalDate date = LocalDate.now();
    Integer duration = contractBatch.getDuration();

    switch (contractBatch.getPeriodTypeSelect()) {
      case ContractBatchRepository.DAYS_PERIOD:
        date = date.plusDays(duration);
        break;

      case ContractBatchRepository.WEEKS_PERIOD:
        date = date.plusWeeks(duration);
        break;

      case ContractBatchRepository.MONTHS_PERIOD:
        date = date.plusMonths(duration);
        break;
    }

    return date;
  }

  @Override
  protected void process() {
    List<ContractVersion> contractVersionList;
    int offset = 0;

    AppContract appContract = (AppContract) appBaseService.getApp("contract");
    Template template = appContract.getContractMessageTemplate();

    Query<ContractVersion> query =
        JPA.all(ContractVersion.class)
            .filter("self.statusSelect = :status AND self.supposedEndDate <= :endOfPeriod")
            .bind("status", ContractBatchRepository.CURRENT_VERSION_ACTIVATION)
            .bind("endOfPeriod", getEndOfPeriod(batch.getContractBatch()))
            .order("id");

    while (!(contractVersionList = query.fetch(FETCH_LIMIT, offset)).isEmpty()) {
      for (ContractVersion contractVersion : contractVersionList) {
        try {
          templateMessageService.generateAndSendMessage(contractVersion, template);
          incrementDone();
        } catch (Exception e) {
          incrementAnomaly();
          TraceBackService.trace(e, "Contract mail reminder batch", batch.getId());
        }
        offset++;
      }
      JPA.clear();
    }
  }

  @Override
  protected void stop() {
    super.stop();
    addComment(
        String.format(
            I18n.get(ITranslation.CONTRACT_BATCH_EXECUTION_RESULT),
            batch.getDone(),
            batch.getAnomaly()));
  }

  @Override
  protected void setBatchTypeSelect() {
    this.batch.setBatchTypeSelect(BatchRepository.BATCH_TYPE_CONTRACT_BATCH);
  }
}
