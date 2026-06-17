/**
 * Spring Modulith application module: websocket.
 * Marked OPEN so its layered sub-packages (domain/application/infrastructure/presentation)
 * are exposed across modules. Verifies no cyclic module dependencies.
 * TODO(refine): tighten to CLOSED + @NamedInterface API exposure + explicit allowedDependencies.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.geekchat.server.websocket;
