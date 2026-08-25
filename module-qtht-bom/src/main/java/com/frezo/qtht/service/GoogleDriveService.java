package com.frezo.qtht.service;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

public interface GoogleDriveService {

    boolean isConfigured();

    String getCredentialsPath();

    String uploadFile(File localFile, String mimeType) throws IOException, GeneralSecurityException;
}
