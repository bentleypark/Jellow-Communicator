package com.dsource.idc.jellowintl;

import android.content.Intent;

import androidx.test.espresso.Espresso;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.dsource.idc.jellowintl.cache.CacheManager;
import com.dsource.idc.jellowintl.factories.TextFactory;
import com.dsource.idc.jellowintl.utility.DataBaseHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.dsource.idc.jellowintl.utility.SessionManager.ENG_IN;
import static com.dsource.idc.jellowintl.utils.FileOperations.copyAssetsToInternalStorage;
import static com.dsource.idc.jellowintl.utils.FileOperations.extractLanguagePackageZipFile;
import static com.dsource.idc.jellowintl.utils.TestClassUtils.getContext;
import static com.dsource.idc.jellowintl.utils.TestClassUtils.getSession;

@RunWith(AndroidJUnit4.class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class _07_ResetPreferencesActivityTest {
    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    @BeforeClass
    public static void setup(){
        getSession().setCaregiverNumber("9653238072");
        getSession().setLanguage(ENG_IN);
        getSession().setGridSize(4);
        copyAssetsToInternalStorage(getContext(), ENG_IN);
        extractLanguagePackageZipFile(getContext(), ENG_IN);
        getSession().setDownloaded(ENG_IN);
        /*DataBaseHelper dbHelper = new DataBaseHelper(getContext());
        dbHelper.createDataBase();
        dbHelper.addLanguageDataToDatabase();
        dbHelper.openDataBase();*/
    }

    @AfterClass
    public static void cleanUp(){
        getSession().setCaregiverNumber("");
        CacheManager.clearCache();
        TextFactory.clearJson();
    }

    @Test
    public void _01_preferencesResetTest(){
        String pref= "1,0,6,0,3,0,60,2,89";
        Espresso.closeSoftKeyboard();
        getSession().setPeoplePreferences(pref);
        DataBaseHelper dbHelper = new DataBaseHelper(getContext());
        dbHelper.delete();
        dbHelper.createDataBase();
        dbHelper.openDataBase();
        dbHelper.addLanguageDataToDatabase();
        dbHelper.setLevel(0,3, pref);
        assert getSession().getPeoplePreferences().equals(pref);
        activityRule.getActivity().startActivity(new Intent
                (activityRule.getActivity(), ResetPreferencesActivity.class));
        onView(withId(R.id.yes)).perform(click());
        assert getSession().getPeoplePreferences().isEmpty();
        assert !dbHelper.getLevel(0, 3).equals(pref);
    }

    /*@Test
    public void _02_backPressedTest(){
        activityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activityRule.getActivity().onBackPressed();
            }
        });

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(activityRule.getActivity().isDestroyed());
    }*/
}
