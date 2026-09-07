package com.caimanproject.payment.core.port.out;

import com.caimanproject.payment.core.port.out.command.StoreProofFileCommand;

public interface ProofFileStorageGateway {

    /**
     * @param command file bytes and metadata to derive the on-disk filename
     * @return the relative file_path to persist, from the configured storage root
     */
    String store(StoreProofFileCommand command);
}
