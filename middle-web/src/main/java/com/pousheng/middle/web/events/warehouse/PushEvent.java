package com.pousheng.middle.web.events.warehouse;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-21
 */
@AllArgsConstructor
public class PushEvent implements Serializable {

    private static final long serialVersionUID = 8485966006501672348L;

    @Getter
    private final Long shopId;
}
