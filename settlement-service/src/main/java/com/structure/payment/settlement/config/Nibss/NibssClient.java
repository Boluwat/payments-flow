package com.structure.payment.settlement.config.Nibss;

import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalResponse;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferResponse;

public interface NibssClient {
    NibssTransferResponse transfer(NibssTransferRequest request);
    NibssReversalResponse reversal(NibssReversalRequest request);
}
