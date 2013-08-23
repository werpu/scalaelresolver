/*
 * Copyright 2013 Werner Punz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.werpu.scalaelresolver.scalaelresolvertest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.io.Serializable;

@Named
@RequestScoped
public class Bean4 implements Serializable {
    private String test4 = "hello world from bean4";

    public String getTest4() {
        return test4;
    }

    public void setTest4(String test4) {
        this.test4 = test4;
    }
}
