package com.icthh.xm.gate.domain.health;

import lombok.Data;
@Data
public class DiskSpace {
    private String status;
    private Long total;
    private Long free;
    private Long threshold;

}
