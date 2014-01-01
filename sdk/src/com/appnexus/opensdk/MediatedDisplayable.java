/*
 *    Copyright 2013 APPNEXUS INC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.appnexus.opensdk;

import android.view.View;

class MediatedDisplayable implements Displayable {
    private View view;
    private MediatedAdViewController mAVC;

    MediatedDisplayable(MediatedAdViewController mAVC) {
        this.mAVC = mAVC;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public boolean failed() {
        return mAVC.hasFailed;
    }

    @Override
    public void destroy() {
        mAVC.finishController();
    }

    void setView(View view) {
        this.view = view;
    }

    MediatedAdViewController getMAVC() {
        return mAVC;
    }

    void setMAVC(MediatedAdViewController mAVC) {
        this.mAVC = mAVC;
    }
}
