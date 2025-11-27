package com.icthh.xm.gate;

import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    GateApp.class,
    SecurityBeanOverrideConfiguration.class
})
public abstract class AbstractSpringBootTest {
}
