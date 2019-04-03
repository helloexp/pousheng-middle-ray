package com.pousheng.middle.common.utils.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by sunbo@terminus.io on 2017/7/24.
 */
@Slf4j
public class AzureOSSBlobClient {

    @Autowired
    private CloudStorageAccount account;
    @Autowired
    private AzureOSSAutoConfiguration.AzureOSSProperties ossProperties;

    public String downloadUrl(String url) {
        try {
            String sufix;
            if (url.endsWith("xlsx")) {
                sufix = ".xlsx";
            } else {
                sufix = ".xls";
            }

            File file = newTempFile("supply-rule-import-temp", "supply-rule-import-", sufix);

            HttpRequest requst = HttpRequest.get(url);
            requst.receive(file);

            return file.getPath();
        } catch (Exception e) {
            log.error("failed to download {} from oss, cause: {}", url, Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    public String download(String fileName, String path) {
        try {
            String sufix;
            if (path.endsWith("xlsx")) {
                sufix = ".xlsx";
            } else {
                sufix = ".xls";
            }
            File file = newTempFile("supply-rule-import-temp", "supply-rule-import-", sufix);

            CloudBlobClient client = account.createCloudBlobClient();
            CloudBlobContainer container = client.getContainerReference(path);

            OperationContext opContext = new OperationContext();
            opContext.setLoggingEnabled(Boolean.TRUE);
            CloudBlob blob = container.getBlobReferenceFromServer(fileName, null, null, null, opContext);
            if (!blob.exists()) {
                return null;
            }

            blob.downloadToFile(file.getPath());
            return file.getPath();
        } catch (Exception e) {
            log.error("failed to download {}/{} from oss, cause: {}", path, fileName, Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    private File newTempFile(String path, String prefix, String suffix) throws IOException {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = File.createTempFile(prefix, suffix, dir);
        log.info("file exist: {}", file.exists());
        log.info("file path: {}", file.getPath());
        if (!log.isDebugEnabled()) {
            file.deleteOnExit();
        }
        return file;
    }

    /**
     * 上传至Azure云存储
     *
     * @param file local file need to upload
     * @param path azure container name
     * @return azure file url
     */
    public String upload(File file, String path) {

        if (!file.exists() || !file.canRead() || file.isDirectory()) {
            throw new ServiceException("azure.oss.upload.file.empty");
        }

        try {
            CloudBlockBlob blob = getBlob(path, file.getName());
            blob.uploadFromFile(file.getPath());
            return blob.getUri().toString();
        } catch (URISyntaxException | StorageException | IOException e) {
            log.error("read file fail,file:{},cause:{}", file.getPath(), Throwables.getStackTraceAsString(e));
            throw new ServiceException("export.read.temp.file.fail");
        }
    }

    /**
     * 上传至Azure云存储
     *
     * @param payload content need to upload
     * @param name    azure file name
     * @param path    azure container name
     * @return azure file url
     */
    public String upload(byte[] payload, String name, String path) {

        if (null == payload || payload.length == 0) {
            throw new ServiceException("azure.oss.upload.content.empty");
        }
        try {
            CloudBlockBlob blob = getBlob(path, name);
            upload(payload, blob);
            return blob.getUri().toString();

        } catch (URISyntaxException | StorageException | IOException e) {
            log.error("upload to azure fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("azure.oss.upload.fail");
        }
    }

    private CloudBlockBlob getBlob(String containerName, String blobName) throws URISyntaxException, StorageException {
        CloudBlobClient client = account.createCloudBlobClient();
        if (null != ossProperties.getTimeout()) {
            client.getDefaultRequestOptions().setMaximumExecutionTimeInMs(ossProperties.getTimeout());
        }
        CloudBlobContainer container = client.getContainerReference(containerName);

        setUpContainer(container);

        return container.getBlockBlobReference(blobName);
    }

    private void upload(byte[] payload, CloudBlockBlob blob) throws IOException, StorageException {
        if (ossProperties.getMultiThreadingUploadThreshold() != null
                && payload.length > ossProperties.getMultiThreadingUploadThreshold().intValue()) {

        } else {
            blob.uploadFromByteArray(payload, 0, payload.length);
        }
    }

    /**
     * 对container设置允许外部读取
     *
     * @param container
     * @throws StorageException
     */
    private void setUpContainer(CloudBlobContainer container) throws StorageException {
        if (!container.exists()) {
            container.create();
            BlobContainerPermissions permissions = new BlobContainerPermissions();
            permissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
            container.uploadPermissions(permissions);
        }
    }
}
