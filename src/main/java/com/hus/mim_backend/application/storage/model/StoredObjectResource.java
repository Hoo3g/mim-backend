package com.hus.mim_backend.application.storage.model;

import java.io.InputStream;

/**
 * Binary object descriptor returned by storage use cases.
 */
public record StoredObjectResource(InputStream stream, String contentType, long size, String originalFilename) {
}
