package com.icthh.xm.gate.cucumber.stepdefs;

import com.icthh.xm.gate.GateApp;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;

import org.springframework.boot.test.context.SpringBootTest;

@WebAppConfiguration
@SpringBootTest
@ContextConfiguration(classes = GateApp.class)
public abstract class StepDefs {

    protected ResultActions actions;

}
