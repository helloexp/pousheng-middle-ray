package com.pousheng.middle.web.item.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.common.utils.component.AzureOSSBlobClient;
import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ShopSkuExcelComponent {

    @Value("${ps.middle.system.gateway}")
    private String psMiddleSystemGateway;

    @Value("${ps.middle.system.accesskey}")
    private String psMiddleSystemAccesskey;

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    public void replaceMaterialIdToBarcodeInExcel(SkuStockRuleImportInfo info) {
        replaceMaterialIdToBarcode(info);
        uploadToAzure(info);
    }

    private void replaceMaterialIdToBarcode(SkuStockRuleImportInfo info) {
        List<String[]> rows = HandlerFileUtil.getInstance().handlerExcel(info.getFilePath());
        List<String> materialIds = rows.stream().map(x -> x[1]).filter(Objects::nonNull).skip(1).distinct().collect(Collectors.toList());
        List<BarcodeResult> results = getBarcode(materialIds);

        List<String[]> newRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (i == 0) {
                newRows.add(new String[]{ "店铺标识(账套-外码)", "货品条码", "限制类型(IN为供货,NOT_IN为不供货)", "限制范围(账套-外码,多个以英文逗号分隔)", "状态(ENABLE为启用,DISABLE为禁用)"});
                continue;
            }

            String materialId = rows.get(i)[1];
            if (materialId != null && results.size() > 0) {
                List<String> barcodes = results.stream()
                        .filter(x -> x.materialId.equalsIgnoreCase(materialId))
                        .flatMap(x -> x.barcodes.stream()).collect(Collectors.toList());

                barcodes.forEach(b -> {
                    newRows.add(new String[]{ row[0], b, row[3], row[4], row[5]});
                });

            } else {
                newRows.add(new String[]{ row[0], row[2], row[3], row[4], row[5]});
            }
        }

        info.setFileName(info.getFileName().replace(".xlsx", "_" + System.currentTimeMillis() + ".xlsx"));

        HandlerFileUtil.getInstance().writerExcel(newRows, info.getFileName());
    }

    public void uploadToAzure(SkuStockRuleImportInfo info) {
        File file = new File(info.getFileName());
        info.setFilePath(azureOssBlobClient.upload(file, "import"));
        file.delete();
    }

    private List<BarcodeResult> getBarcode(List<String> materialIds) {
        String url = psMiddleSystemGateway + "/middle-system-api/v1/skutemplate/barcode?material_ids=" + String.join(",", materialIds);

        String resp = HttpRequest.get(url).header("access-key", psMiddleSystemAccesskey).body();
        BarcodeMapping barcodes =  JsonMapper.nonDefaultMapper().fromJson(resp, BarcodeMapping.class);

        return barcodes.result;
    }

    @Data
    private static class BarcodeMapping {

        @JsonProperty(value = "retCode")
        private int retCode;

        @JsonProperty(value = "retMessage")
        private String retMessage;

        @JsonProperty(value = "result")
        private List<BarcodeResult> result;
    }

    @Data
    private static class BarcodeResult implements Serializable {

        @JsonProperty(value = "material_id")
        private String materialId;

        @JsonProperty(value = "barcodes")
        private List<String> barcodes;
    }
}
