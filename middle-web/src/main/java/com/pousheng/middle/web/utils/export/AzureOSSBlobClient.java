package com.pousheng.middle.web.utils.export;

import com.google.common.base.Throwables;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.sql.Blob;
import java.util.Optional;

/**
 * Created by sunbo@terminus.io on 2017/7/24.
 */
@Slf4j
public class AzureOSSBlobClient {

    @Autowired
    private CloudStorageAccount account;

    public String upload(File file, String path) {

        if (!file.exists() || !file.canRead() || file.isDirectory())
            throw new ServiceException("azure.oss.upload.file.empty");

        CloudBlobClient client = account.createCloudBlobClient();
        try {
            CloudBlobContainer container = client.getContainerReference(path);
            setUpContainer(container);

            CloudBlockBlob blob = container.getBlockBlobReference(file.getName());
            blob.uploadFromFile(file.getPath());
            return blob.getUri().toString();

        } catch (URISyntaxException | StorageException | IOException e) {
            log.error("upload to azure fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("azure.oss.upload.fail");
        }
    }

    public String upload(byte[] payload, String name, String path) {

        if (null == payload || payload.length == 0)
            throw new ServiceException("azure.oss.upload.content.empty");

        CloudBlobClient client = account.createCloudBlobClient();

        try {
            CloudBlobContainer container = client.getContainerReference(path);
            setUpContainer(container);

            CloudBlockBlob blob = container.getBlockBlobReference(name);
            blob.uploadFromByteArray(payload, 0, payload.length);
            return blob.getUri().toString();

        } catch (URISyntaxException | StorageException | IOException e) {
            log.error("upload to azure fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("azure.oss.upload.fail");
        }
    }


    private void setUpContainer(CloudBlobContainer container) throws StorageException {
        if (!container.exists()) {
            container.create();
            BlobContainerPermissions permissions = new BlobContainerPermissions();
            permissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
            container.uploadPermissions(permissions);
        }
    }
}
