/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml), Marcelina Knitter (@marcelinkaaa)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;

import com.frostwire.android.R;
import com.frostwire.android.gui.fragments.TransferDetailDetailsFragment;
import com.frostwire.android.gui.fragments.TransferDetailFilesFragment;
import com.frostwire.android.gui.fragments.TransferDetailPeersFragment;
import com.frostwire.android.gui.fragments.TransferDetailPiecesFragment;
import com.frostwire.android.gui.fragments.TransferDetailStatusFragment;
import com.frostwire.android.gui.fragments.TransferDetailTrackersFragment;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.transfers.UIBittorrentDownload;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractTransferDetailFragment;
import com.frostwire.android.gui.views.TimerObserver;
import com.frostwire.android.gui.views.TimerService;
import com.frostwire.android.gui.views.TimerSubscription;

import java.util.List;

public class TransferDetailActivity extends AbstractActivity implements TimerObserver {
    private TimerSubscription subscription;
    private UIBittorrentDownload uiBittorrentDownload;
    private SparseArray<String> tabTitles;
    private AbstractTransferDetailFragment[] detailFragments;
    private int lastSelectedTabIndex;

    public TransferDetailActivity() {
        super(R.layout.activity_transfer_detail);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        super.initComponents(savedInstanceState);
        initUIBittorrentDownload();
        initTabTitles();
        initFragments();
        if (detailFragments == null || detailFragments.length <= 0) {
            throw new RuntimeException("check your logic: can't initialize components without initialized fragments");
        }
        OnPageChangeListener onPageChangeListener = new OnPageChangeListener(this);
        SectionsPagerAdapter mSectionsPagerAdapter =
                new SectionsPagerAdapter(getSupportFragmentManager(), detailFragments);
        ViewPager viewPager = findViewById(R.id.transfer_detail_viewpager);
        if (viewPager != null) {
            viewPager.clearOnPageChangeListeners();
            viewPager.setAdapter(mSectionsPagerAdapter);
            viewPager.setCurrentItem(0);
            if (savedInstanceState != null) {
                int lastSelectedTabIndex = savedInstanceState.getInt("lastSelectedTabIndex", -1);
                if (lastSelectedTabIndex != -1) {
                    viewPager.setCurrentItem(lastSelectedTabIndex);
                }
            }
            viewPager.addOnPageChangeListener(onPageChangeListener);
            TabLayout tabLayout = findViewById(R.id.transfer_detail_tab_layout);
            tabLayout.setupWithViewPager(viewPager);
        } else {
            throw new RuntimeException("initComponents() Could not get viewPager");
        }
    }

    private void initUIBittorrentDownload() {
        String infoHash = getIntent().getStringExtra("infoHash");
        if (infoHash == null || "".equals(infoHash)) {
            throw new RuntimeException("Invalid infoHash received");
        }
        uiBittorrentDownload = (UIBittorrentDownload)
                TransferManager.instance().getBittorrentDownload(infoHash);
        if (uiBittorrentDownload == null) {
            throw new RuntimeException("Could not find matching transfer for infoHash:" + infoHash);
        }
    }

    private void initTabTitles() {
        tabTitles = new SparseArray<>(6);
        tabTitles.put(R.string.files, getString(R.string.files));
        tabTitles.put(R.string.status, getString(R.string.status));
        tabTitles.put(R.string.details, getString(R.string.details));
        tabTitles.put(R.string.trackers, getString(R.string.trackers));
        tabTitles.put(R.string.peers, getString(R.string.peers));
        tabTitles.put(R.string.pieces, getString(R.string.pieces));
    }

    private void initFragments() {
        if (uiBittorrentDownload == null) {
            throw new RuntimeException("check your logic: can't init fragments without an uiBitTorrentDownload instance set");
        }
        if (tabTitles == null || tabTitles.size() <= 0) {
            throw new RuntimeException("check your logic: can't init fragments without initializing tab titles");
        }

        // If you need to change the order of the tabs, just change this array
        // the rest should be taken care of automatically
        Class[] detailFragmentClasses = new Class[] {
                TransferDetailFilesFragment.class,
                TransferDetailStatusFragment.class,
                TransferDetailDetailsFragment.class,
                TransferDetailTrackersFragment.class,
                TransferDetailPeersFragment.class,
                TransferDetailPiecesFragment.class
        };
        detailFragments = new AbstractTransferDetailFragment[detailFragmentClasses.length];

        // are we rotating? if so, we may just recover our pre-existing fragments and put them here.
        List<Fragment> knownFragments = getFragments();
        // we ask > 1 because [0] is always some framework tracked fragment
        if (knownFragments != null && knownFragments.size() > 1) {
            recoverExistingFragments(detailFragmentClasses);
        } else {
            // to change the order of the tabs, add/remove tabs, just maintain here.
            int i = 0;
            for (Class clazz : detailFragmentClasses) {
                try {
                    detailFragments[i++] = (AbstractTransferDetailFragment) clazz.newInstance();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        // make sure all fragments
        for (AbstractTransferDetailFragment f : detailFragments) {
            f.init(this, tabTitles, uiBittorrentDownload);
        }
    }

    private void recoverExistingFragments(Class[] detailFragmentClasses) {
        int i = 0;
        for (Class fragmentClass : detailFragmentClasses) {
            Fragment correspondingActiveFragment = getCorrespondingActiveFragment(fragmentClass);
            if (correspondingActiveFragment != null && correspondingActiveFragment.isAdded()) {
                detailFragments[i] = (AbstractTransferDetailFragment) correspondingActiveFragment;
            } else {
                try {
                    // instantiate a new one
                    detailFragments[i] = (AbstractTransferDetailFragment) fragmentClass.newInstance();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            i++;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
        if (uiBittorrentDownload == null) {
            throw new RuntimeException("No UIBittorrent download, unacceptable");
        }
        subscription = TimerService.subscribe(this, 2);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    public void onTime() {
        if (subscription == null || subscription.isUnsubscribed()) {
            return;
        }
        if (detailFragments == null || detailFragments.length == 0) {
            return;
        }
        if (lastSelectedTabIndex < 0 || lastSelectedTabIndex > detailFragments.length - 1) {
            return;
        }
        AbstractTransferDetailFragment currentFragment = detailFragments[lastSelectedTabIndex];
        if (currentFragment == null) {
            return;
        }
        if (!currentFragment.isAdded()) {
            Fragment correspondingActiveFragment = getCorrespondingActiveFragment(currentFragment.getClass());
            if (correspondingActiveFragment == null) {
                return; // definitively not added yet
            }
            detailFragments[lastSelectedTabIndex] = (AbstractTransferDetailFragment) correspondingActiveFragment;
            currentFragment = detailFragments[lastSelectedTabIndex];
        }
        try {
            currentFragment.onTime();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Fragment rotation ends up with initialized detail fragments not added,
     * it seems the SectionsPageAdapter doesn't properly tag the fragments
     * and we have to manually find the corresponding added fragment
     * in the list keep by AbstractFragment's getFragments()
     * <p>
     * We receive a fragment whose .isAdded() method returns false and we
     * look into our tracked list of fragments for an equivalent instance that
     * is marked as added and return it.
     * <p>
     * We'll then replace that instance in our detailFragments[] array
     */
    private Fragment getCorrespondingActiveFragment(Class fragmentClass) {
        List<Fragment> fragments = getFragments();
        if (fragments.size() > 1) {
            for (Fragment f : fragments) {
                if (f.isAdded() && fragmentClass == f.getClass()) {
                    return f;
                }
            }
        }
        return null;
    }

    private void onTabSelected(int position) {
        lastSelectedTabIndex = position;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("lastSelectedTabIndex", lastSelectedTabIndex);
    }

    private static class SectionsPagerAdapter extends android.support.v4.app.FragmentPagerAdapter {

        private final android.support.v4.app.Fragment[] detailFragments;

        public SectionsPagerAdapter(android.support.v4.app.FragmentManager supportFM,
                                    android.support.v4.app.Fragment[] detailFragments) {
            super(supportFM);
            this.detailFragments = detailFragments;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return detailFragments[position];
        }

        @Override
        public int getCount() {
            return detailFragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return ((AbstractTransferDetailFragment)detailFragments[position]).getTabTitle().toUpperCase();
        }
    }

    private static final class OnPageChangeListener implements ViewPager.OnPageChangeListener {
        private final TransferDetailActivity activity;

        OnPageChangeListener(TransferDetailActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            try {
                activity.onTabSelected(position);
                activity.onTime();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}
