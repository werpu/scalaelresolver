package com.github.werpu.scalaelresolver.scalaelresolvertest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@Named
@RequestScoped
public class Bean4 {
    private String test4 = "hello world from bean4";

    public String getTest4() {
        return test4;
    }

    public void setTest4(String test4) {
        this.test4 = test4;
    }
}
