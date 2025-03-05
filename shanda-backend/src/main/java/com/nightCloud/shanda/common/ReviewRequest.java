package com.nightCloud.shanda.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 审核状态
 */
@Data
public class ReviewRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 审核状态 0-未审核 1-审核通过 2-审核拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    private static final long serialVersionUID = 1L;
}
