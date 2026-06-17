package com.icthh.xm.gate.listener;

import com.icthh.xm.commons.messaging.event.system.SystemEvent;

public abstract class SystemTopicAbstractHandler {

    public abstract boolean supports(String eventType);

    public abstract void execute(SystemEvent event);
}
