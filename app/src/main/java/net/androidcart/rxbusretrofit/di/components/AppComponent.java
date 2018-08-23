package net.androidcart.rxbusretrofit.di.components;

import net.androidcart.rxbusretrofit.BaseActivity;
import net.androidcart.rxbusretrofit.MainActivity;
import net.androidcart.rxbusretrofit.MyApp;
import net.androidcart.rxbusretrofit.di.modules.AppModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by Amin Amini on 8/23/18.
 */

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {
    void inject(MyApp myApp);
    void inject(BaseActivity baseActivity);
}
