package net.createmod.catnip.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Marks API that must only be linked from the physical client. */
@Retention(RetentionPolicy.CLASS)
public @interface ClientOnly {
}
