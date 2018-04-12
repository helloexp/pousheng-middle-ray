package com.pousheng.middle.web.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2018/3/27
 */
@Data
public class ShopChannelGroup implements Serializable{

    private static final long serialVersionUID = 1681180986855125363L;

    private String channel;

    private List<ShopChannel> shopChannels;


}
