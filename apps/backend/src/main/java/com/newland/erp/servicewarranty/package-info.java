@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "sales::integration",
      "productcatalog::integration",
      "inventory::integration",
      "enterprise::integration",
      "identity::integration",
      "platform::integration"
    })
package com.newland.erp.servicewarranty;
