package com.newland.erp.logistics.application;

public interface LogisticsMasterReferencePort {
  void requireActiveCarrier(String carrierCode);

  void requireActivePort(String portCode);

  void requireActiveIncoterm(String incotermCode);
}
