package com.pousheng.middle.web.utils.export;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
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
            throw new RuntimeException("file not exist or can't read or is a directory");

        CloudBlobClient client = account.createCloudBlobClient();
        try {
            CloudBlobContainer container = client.getContainerReference(path);
            setUpContainer(container);

            CloudBlockBlob blob = container.getBlockBlobReference(file.getName());
            blob.uploadFromFile(file.getPath());
            return blob.getUri().toString();

        } catch (URISyntaxException e) {
            throw new RuntimeException();
        } catch (StorageException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public String upload(byte[] playload, String name, String path) {

        if (null == playload || playload.length == 0)
            throw new RuntimeException("update content is empty");

        CloudBlobClient client = account.createCloudBlobClient();

        try {
            CloudBlobContainer container = client.getContainerReference(path);
            setUpContainer(container);

            CloudBlockBlob blob = container.getBlockBlobReference(name);
            blob.uploadFromByteArray(playload, 0, playload.length);
            return blob.getUri().toString();

        } catch (URISyntaxException e) {
            throw new RuntimeException();
        } catch (StorageException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
