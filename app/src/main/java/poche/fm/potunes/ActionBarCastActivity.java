/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package poche.fm.potunes;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.ToggleDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import poche.fm.potunes.fragment.LocalDownloadAlbumFragment;
import poche.fm.potunes.utils.UpdateUtil;


public abstract class ActionBarCastActivity extends AppCompatActivity {
    private String TAG = "ActionbarActivity";

    private Toolbar mToolbar;
    public TextView mToolbarTitle;
    private boolean mToolbarInitialized;
    private Drawer result = null;
    private ActionBarDrawerToggle mDrawerToggle;

    private final FragmentManager.OnBackStackChangedListener mBackStackChangedListener =
        new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateDrawerToggle();
            }
        };

    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
            if (drawerItem instanceof Nameable) {
                Toast.makeText(getApplicationContext(), R.string.not_available, Toast.LENGTH_SHORT).show();
            } else {
                Log.i("material-drawer", "toggleChecked: " + isChecked);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                "the end of your onCreate method");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Whenever the fragment back stack changes, we may need to update the
        // action bar toggle: only top level screens show the hamburger-like icon, inner
        // screens - either Activities or fragments - show the "Up" icon instead.
        getSupportFragmentManager().addOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getSupportFragmentManager().removeOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public void onBackPressed() {
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            // If the drawer is open, back will close it
            // Otherwise, it may return to the previous fragment stack
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.getBackStackEntryCount() > 0) {
                fragmentManager.popBackStackImmediate();
            } else {
                // Lastly, it will rely on the system behavior for back
                moveTaskToBack(true);
            }
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            moveTaskToBack(true);
        }
        return false;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbarTitle.setText(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    public String getToolbarTitle() {
        return mToolbarTitle.getText().toString();
    }

    protected void initializeToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                "'toolbar'");
        }
        setSupportActionBar(mToolbar);
        mToolbarTitle = (TextView) findViewById(R.id.app_title);
        mToolbarInitialized = true;
        createDrawer();
    }

    protected void createDrawer() {
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_item_home);
        SwitchDrawerItem item2 = new SwitchDrawerItem().withIdentifier(2).withChecked(true).withName(R.string.drawer_item_mobile_play).withOnCheckedChangeListener(onCheckedChangeListener);
        SwitchDrawerItem item3 = new SwitchDrawerItem().withIdentifier(3).withChecked(true).withName(R.string.drawer_item_mobile_download).withOnCheckedChangeListener(onCheckedChangeListener);
        SwitchDrawerItem item4 = new SwitchDrawerItem().withIdentifier(4).withChecked(true).withName(R.string.drawer_item_lock_screen).withOnCheckedChangeListener(onCheckedChangeListener);
        PrimaryDrawerItem item5= new PrimaryDrawerItem().withIdentifier(5).withName(R.string.drawer_item_check_update);

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
                        new ProfileDrawerItem().withName("Purchas Raul").withEmail("me@poche.fm").withIcon(getResources().getDrawable(R.drawable.profile))
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

        result = new DrawerBuilder()
                .withAccountHeader(headerResult)
                .withActivity(this)
                .withToolbar(mToolbar)
                .addDrawerItems(
                        item1,
                        new DividerDrawerItem(),
                        item2,
                        item3,
                        item4,
                        item5
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        switch (position) {
                            case 1:
                                Bundle extras = ActivityOptions.makeCustomAnimation(
                                        ActionBarCastActivity.this, R.anim.fade_in, R.anim.fade_out).toBundle();
                                startActivity(new Intent(ActionBarCastActivity.this, MainActivity.class), extras);
                                finish();
                                break;
                            // 检查更新
                            case 6:
                                Toast.makeText(getApplicationContext(), R.string.check_update, Toast.LENGTH_LONG).show();
                                UpdateUtil update = new UpdateUtil(ActionBarCastActivity.this);
                                update.checkUpdate(false);
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), R.string.not_available, Toast.LENGTH_SHORT).show();
                                break;
                        }
                        return false;
                    }
                })
                .build();
        mDrawerToggle = new ActionBarDrawerToggle(this, result.getDrawerLayout(),
                mToolbar, R.string.open_content_drawer, R.string.close_content_drawer);
        mDrawerToggle.syncState();
    }
    protected void updateDrawerToggle() {
        if (mDrawerToggle == null) {
            return;
        }
        boolean isRoot = getSupportFragmentManager().getBackStackEntryCount() == 0;
        Log.d("", "updateDrawerToggle: " + getSupportFragmentManager().getBackStackEntryCount());

        mDrawerToggle.setDrawerIndicatorEnabled(isRoot);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(!isRoot);
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isRoot);
            getSupportActionBar().setHomeButtonEnabled(!isRoot);
        }

        if (isRoot) {
            if (mToolbarTitle != null) {
                mToolbarTitle.setText(R.string.app_name);
            }
        } else {
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isRoot = getSupportFragmentManager().getBackStackEntryCount() == 0;
                    if (isRoot) {
                        result.openDrawer();
                    } else {
                        onBackPressed();
                    }
                }
            });
        }
        if (isRoot) {
            mDrawerToggle.syncState();
        }
    }
}
