@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sales::integration",
      "enterprise::integration",
      "identity::integration",
      "platform::integration"
    })
package com.newland.erp.crm;
