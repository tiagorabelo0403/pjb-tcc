package com.tcc.pjb.backend.service.magistratura.acts;

record DeskTarget(String stageToken,
                          String inboxKey,
                          String queueCode,
                          String panelRoute,
                          String cellCode,
                          String bindingKey) {
}
