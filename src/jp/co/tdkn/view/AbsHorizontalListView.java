/*
 * Copyright (C) 2006 The Android Open Source Project
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

package jp.co.tdkn.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.co.tdkn.util.ReflectionProxy;
import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Debug;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.StateSet;
import android.view.ActionMode;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Adapter;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.OverScroller;
import android.widget.PopupWindow;

/**
 * Base class that can be used to implement virtualized lists of items. A list
 * does not have a spatial definition here. For instance, subclases of this
 * class can display the content of the list in a grid, in a carousel, as stack,
 * etc.
 * 
 * @attr ref android.R.styleable#AbsListView_listSelector
 * @attr ref android.R.styleable#AbsListView_drawSelectorOnTop
 * @attr ref android.R.styleable#AbsListView_stackFromBottom
 * @attr ref android.R.styleable#AbsListView_scrollingCache
 * @attr ref android.R.styleable#AbsListView_textFilterEnabled
 * @attr ref android.R.styleable#AbsListView_transcriptMode
 * @attr ref android.R.styleable#AbsListView_cacheColorHint
 * @attr ref android.R.styleable#AbsListView_fastScrollEnabled
 * @attr ref android.R.styleable#AbsListView_smoothScrollbar
 * @attr ref android.R.styleable#AbsListView_choiceMode
 */
public abstract class AbsHorizontalListView extends AdapterView<ListAdapter>
        implements ViewTreeObserver.OnTouchModeChangeListener {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "AbsHorizontalListView";

    /**
     * When set, this ViewGroup should not intercept touch events. {@hide
     * 
     * 
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     * }
     */
    protected static final int FLAG_DISALLOW_INTERCEPT = 0x80000;

    /**
     * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
     * should add only accessibility focusable Views.
     * 
     * @hide
     */
    public static final int FOCUSABLES_ACCESSIBILITY = 0x00000002;

    /**
     * Disables the transcript mode.
     * 
     * @see #setTranscriptMode(int)
     */
    public static final int TRANSCRIPT_MODE_DISABLED = 0;
    /**
     * The list will automatically scroll to the bottom when a data set change
     * notification is received and only if the last item is already visible on
     * screen.
     * 
     * @see #setTranscriptMode(int)
     */
    public static final int TRANSCRIPT_MODE_NORMAL = 1;
    /**
     * The list will automatically scroll to the bottom, no matter what items
     * are currently visible.
     * 
     * @see #setTranscriptMode(int)
     */
    public static final int TRANSCRIPT_MODE_ALWAYS_SCROLL = 2;

    /**
     * Indicates that we are not in the middle of a touch gesture
     */
    static final int TOUCH_MODE_REST = -1;

    /**
     * Indicates we just received the touch event and we are waiting to see if
     * the it is a tap or a scroll gesture.
     */
    static final int TOUCH_MODE_DOWN = 0;

    /**
     * Indicates the touch has been recognized as a tap and we are now waiting
     * to see if the touch is a longpress
     */
    static final int TOUCH_MODE_TAP = 1;

    /**
     * Indicates we have waited for everything we can wait for, but the user's
     * finger is still down
     */
    static final int TOUCH_MODE_DONE_WAITING = 2;

    /**
     * Indicates the touch gesture is a scroll
     */
    static final int TOUCH_MODE_SCROLL = 3;

    /**
     * Indicates the view is in the process of being flung
     */
    static final int TOUCH_MODE_FLING = 4;

    /**
     * Indicates the touch gesture is an overscroll - a scroll beyond the
     * beginning or end.
     */
    static final int TOUCH_MODE_OVERSCROLL = 5;

    /**
     * Indicates the view is being flung outside of normal content bounds and
     * will spring back.
     */
    static final int TOUCH_MODE_OVERFLING = 6;

    /**
     * Regular layout - usually an unsolicited layout from the view system
     */
    static final int LAYOUT_NORMAL = 0;

    /**
     * Show the first item
     */
    static final int LAYOUT_FORCE_LEFT = 1;

    /**
     * Force the selected item to be on somewhere on the screen
     */
    static final int LAYOUT_SET_SELECTION = 2;

    /**
     * Show the last item
     */
    static final int LAYOUT_FORCE_RIGHT = 3;

    /**
     * Make a mSelectedItem appear in a specific location and build the rest of
     * the views from there. The left is specified by mSpecificLeft.
     */
    static final int LAYOUT_SPECIFIC = 4;

    /**
     * Layout to sync as a result of a data change. Restore mSyncPosition to
     * have its left at mSpecificLeft
     */
    static final int LAYOUT_SYNC = 5;

    /**
     * Layout as a result of using the navigation keys
     */
    static final int LAYOUT_MOVE_SELECTION = 6;

    /**
     * Normal list that does not indicate choices
     */
    public static final int CHOICE_MODE_NONE = 0;

    /**
     * The list allows up to one choice
     */
    public static final int CHOICE_MODE_SINGLE = 1;

    /**
     * The list allows multiple choices
     */
    public static final int CHOICE_MODE_MULTIPLE = 2;

    /**
     * The list allows multiple choices in a modal selection mode
     */
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;

    /**
     * Controls if/how the user may choose/check items in the list
     */
    int mChoiceMode = CHOICE_MODE_NONE;

    /**
     * Controls CHOICE_MODE_MULTIPLE_MODAL. null when inactive.
     */
    ActionMode mChoiceActionMode;

    /**
     * Wrapper for the multiple choice mode callback; AbsHorizontalListView
     * needs to perform a few extra actions around what application code does.
     */
    MultiChoiceModeWrapper mMultiChoiceModeCallback;

    /**
     * Running count of how many items are currently checked
     */
    int mCheckedItemCount;

    /**
     * Running state of which positions are currently checked
     */
    SparseBooleanArray mCheckStates;

    /**
     * Running state of which IDs are currently checked. If there is a value for
     * a given key, the checked state for that ID is true and the value holds
     * the last known position in the adapter for that id.
     */
    HashMap<Long, Integer> mCheckedIdStates;

    /**
     * Controls how the next layout will happen
     */
    int mLayoutMode = LAYOUT_NORMAL;

    /**
     * Should be used by subclasses to listen to changes in the dataset
     */
    AdapterDataSetObserver mDataSetObserver;

    /**
     * The adapter containing the data to be displayed by this view
     */
    ListAdapter mAdapter;

    /**
     * If mAdapter != null, whenever this is true the adapter has stable IDs.
     */
    boolean mAdapterHasStableIds;

    /**
     * This flag indicates the a full notify is required when the
     * RemoteViewsAdapter connects
     */
    private boolean mDeferNotifyDataSetChanged = false;

    /**
     * Indicates whether the list selector should be drawn on left of the
     * children or behind
     */
    boolean mDrawSelectorOnLeft = false;

    /**
     * The drawable used to draw the selector
     */
    Drawable mSelector;

    /**
     * The current position of the selector in the list.
     */
    int mSelectorPosition = INVALID_POSITION;

    /**
     * Defines the selector's location and dimension at drawing time
     */
    Rect mSelectorRect = new Rect();

    /**
     * The data set used to store unused views that should be reused during the
     * next layout to avoid creating new ones
     */
    final RecycleBin mRecycler = new RecycleBin();

    /**
     * The selection's left padding
     */
    int mSelectionLeftPadding = 0;

    /**
     * The selection's top padding
     */
    int mSelectionTopPadding = 0;

    /**
     * The selection's right padding
     */
    int mSelectionRightPadding = 0;

    /**
     * The selection's bottom padding
     */
    int mSelectionBottomPadding = 0;

    /**
     * This view's padding
     */
    Rect mListPadding = new Rect();

    /**
     * Subclasses must retain their measure spec from onMeasure() into this
     * member
     */
    int mHeightMeasureSpec = 0;

    /**
     * The left scroll indicator
     */
    View mScrollLeft;

    /**
     * The right scroll indicator
     */
    View mScrollRight;

    /**
     * When the view is scrolling, this flag is set to true to indicate
     * subclasses that the drawing cache was enabled on the children
     */
    boolean mCachingStarted;
    boolean mCachingActive;

    /**
     * The position of the view that received the down motion event
     */
    int mMotionPosition;

    /**
     * The offset to the left of the mMotionPosition view when the down motion
     * event was received
     */
    int mMotionViewOriginalLeft;

    /**
     * The desired offset to the left of the mMotionPosition view after a scroll
     */
    int mMotionViewNewLeft;

    /**
     * The X value associated with the the down motion event
     */
    int mMotionX;

    /**
     * The Y value associated with the the down motion event
     */
    int mMotionY;

    /**
     * One of TOUCH_MODE_REST, TOUCH_MODE_DOWN, TOUCH_MODE_TAP,
     * TOUCH_MODE_SCROLL, or TOUCH_MODE_DONE_WAITING
     */
    int mTouchMode = TOUCH_MODE_REST;

    /**
     * X value from on the previous motion event (if any)
     */
    int mLastX;

    /**
     * How far the finger moved before we started scrolling
     */
    int mMotionCorrection;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * Handles one frame of a fling
     */
    private FlingRunnable mFlingRunnable;

    /**
     * Handles scrolling between positions within the list.
     */
    PositionScroller mPositionScroller;

    /**
     * The offset in pixels form the left of the AdapterView to the left of the
     * currently selected view. Used to save and restore state.
     */
    int mSelectedLeft = 0;

    /**
     * Indicates whether the list is stacked from the right edge or the top
     * edge.
     */
    boolean mStackFromRight;

    /**
     * When set to true, the list automatically discards the children's bitmap
     * cache after scrolling.
     */
    boolean mScrollingCacheEnabled;

    /**
     * Optional callback to notify client when scroll position has changed
     */
    private OnScrollListener mOnScrollListener;

    /**
     * Keeps track of our accessory window
     */
    PopupWindow mPopup;

    /**
     * Used with type filter window
     */
    EditText mTextFilter;

    /**
     * Indicates whether to use pixels-based or position-based scrollbar
     * properties.
     */
    private boolean mSmoothScrollbarEnabled = true;

    /**
     * Indicates that this view supports filtering
     */
    private boolean mTextFilterEnabled;

    /**
     * Indicates that this view is currently displaying a filtered view of the
     * data
     */
    private boolean mFiltered;

    /**
     * Rectangle used for hit testing children
     */
    private Rect mTouchFrame;

    /**
     * The position to resurrect the selected position to.
     */
    int mResurrectToPosition = INVALID_POSITION;

    private ContextMenuInfo mContextMenuInfo = null;

    /**
     * Maximum distance to record overscroll
     */
    int mOverscrollMax;

    /**
     * Content height divided by this is the overscroll limit.
     */
    static final int OVERSCROLL_LIMIT_DIVISOR = 3;

    /**
     * How many positions in either direction we will search to try to find a
     * checked item with a stable ID that moved position across a data set
     * change. If the item isn't found it will be unselected.
     */
    private static final int CHECK_POSITION_SEARCH_DISTANCE = 20;

    /**
     * Used to request a layout when we changed touch mode
     */
    private static final int TOUCH_MODE_UNKNOWN = -1;
    private static final int TOUCH_MODE_ON = 0;
    private static final int TOUCH_MODE_OFF = 1;

    private int mLastTouchMode = TOUCH_MODE_UNKNOWN;

    private static final boolean PROFILE_SCROLLING = false;
    private boolean mScrollProfilingStarted = false;

    private static final boolean PROFILE_FLINGING = false;
    private boolean mFlingProfilingStarted = false;

    /**
     * The last CheckForLongPress runnable we posted, if any
     */
    private CheckForLongPress mPendingCheckForLongPress;

    /**
     * The last CheckForTap runnable we posted, if any
     */
    private Runnable mPendingCheckForTap;

    /**
     * The last CheckForKeyLongPress runnable we posted, if any
     */
    private CheckForKeyLongPress mPendingCheckForKeyLongPress;

    /**
     * Acts upon click
     */
    private AbsHorizontalListView.PerformClick mPerformClick;

    /**
     * Delayed action for touch mode.
     */
    private Runnable mTouchModeReset;

    /**
     * This view is in transcript mode -- it shows the bottom of the list when
     * the data changes
     */
    private int mTranscriptMode;

    /**
     * Indicates that this list is always drawn on top of a solid, single-color,
     * opaque background
     */
    private int mCacheColorHint;

    /**
     * The select child's view (from the adapter's getView) is enabled.
     */
    private boolean mIsChildViewEnabled;

    /**
     * The last scroll state reported to clients through
     * {@link OnScrollListener}.
     */
    private int mLastScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    private int mTouchSlop;

    private Runnable mClearScrollingCache;
    Runnable mPositionScrollAfterLayout;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private float mVelocityScale = 1.0f;

    final boolean[] mIsScrap = new boolean[1];

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    /**
     * Sentinel value for no current active pointer. Used by
     * {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    /**
     * Maximum distance to overscroll by during edge effects
     */
    int mOverscrollDistance;

    /**
     * Maximum distance to overfling during edge effects
     */
    int mOverflingDistance;

    /**
     * An estimate of how many pixels are between the top of the list and the
     * top of the first position in the adapter, based on the last time we saw
     * it. Used to hint where to draw edge glows.
     */
    private int mFirstPositionDistanceGuess;

    /**
     * An estimate of how many pixels are between the bottom of the list and the
     * bottom of the last position in the adapter, based on the last time we saw
     * it. Used to hint where to draw edge glows.
     */
    private int mLastPositionDistanceGuess;

    /**
     * Used for determining when to cancel out of overscroll.
     */
    private int mDirection = 0;

    /**
     * Tracked on measurement in transcript mode. Makes sure that we can still
     * pin to the bottom correctly on resizes.
     */
    private boolean mForceTranscriptScroll;

    /**
     * Track if we are currently attached to a window.
     */
    boolean mIsAttached;

    /**
     * Track the item count from the last time we handled a data change.
     */
    private int mLastHandledItemCount;

    protected final HideApiProxy mHideApiProxy = new HideApiProxy();

    /**
     * Used for smooth scrolling at a consistent rate
     */
    static final Interpolator sLinearInterpolator = new LinearInterpolator();

    /**
     * Interface definition for a callback to be invoked when the list or grid
     * has been scrolled.
     */
    public interface OnScrollListener {

        /**
         * The view is not scrolling. Note navigating the list using the
         * trackball counts as being in the idle state since these transitions
         * are not animated.
         */
        public static int SCROLL_STATE_IDLE = 0;

        /**
         * The user is scrolling using touch, and their finger is still on the
         * screen
         */
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;

        /**
         * The user had previously been scrolling using touch and had performed
         * a fling. The animation is now coasting to a stop
         */
        public static int SCROLL_STATE_FLING = 2;

        /**
         * Callback method to be invoked while the list view or grid view is
         * being scrolled. If the view is being scrolled, this method will be
         * called before the next frame of the scroll is rendered. In
         * particular, it will be called before any calls to
         * {@link Adapter#getView(int, View, ViewGroup)}.
         * 
         * @param view
         *            The view whose scroll state is being reported
         * 
         * @param scrollState
         *            The current scroll state. One of
         *            {@link #SCROLL_STATE_IDLE},
         *            {@link #SCROLL_STATE_TOUCH_SCROLL} or
         *            {@link #SCROLL_STATE_IDLE}.
         */
        public void onScrollStateChanged(AbsHorizontalListView view,
                int scrollState);

        /**
         * Callback method to be invoked when the list or grid has been
         * scrolled. This will be called after the scroll has completed
         * 
         * @param view
         *            The view whose scroll state is being reported
         * @param firstVisibleItem
         *            the index of the first visible cell (ignore if
         *            visibleItemCount == 0)
         * @param visibleItemCount
         *            the number of visible cells
         * @param totalItemCount
         *            the number of items in the list adaptor
         */
        public void onScroll(AbsHorizontalListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount);
    }

    /**
     * The top-level view of a list item can implement this interface to allow
     * itself to modify the bounds of the selection shown for that item.
     */
    public interface SelectionBoundsAdjuster {
        /**
         * Called to allow the list item to adjust the bounds shown for its
         * selection.
         * 
         * @param bounds
         *            On call, this contains the bounds the list has selected
         *            for the item (that is the bounds of the entire view). The
         *            values can be modified as desired.
         */
        public void adjustListItemSelectionBounds(Rect bounds);
    }

    /**
     * 非公開 API へアクセスするためのラップクラス
     * 
     * @tdkn
     */
    static final class HideApiProxy extends ReflectionProxy {

        int mGroupFlags(ViewGroup instance) {
            return getFieldValueAsInt(ViewGroup.class, "mGroupFlags", instance);
        }

        void mGroupFlags(ViewGroup instance, int value) {
            setFieldValue(ViewGroup.class, "mGroupFlags", value, instance);
        }

        void mLeft(View instance, int value) {
            setFieldValue(View.class, "mLeft", value, instance);
        }

        void mRight(View instance, int value) {
            setFieldValue(View.class, "mRight", value, instance);
        }

        void mScrollX(View instance, int scrollX) {
            setFieldValue(View.class, "mScrollX", scrollX, instance);
        }

        boolean mCachingFailed(View view) {
            return getFieldValueAsBoolean(View.class, "mCachingFailed", view);
        }

        boolean hasOpaqueScrollbars(View view) {
            return (Boolean) invoke(View.class, "hasOpaqueScrollbars", view,
                    null, null);
        }

        void dispatchStartTemporaryDetach(View instance) {
            invoke(View.class, "dispatchStartTemporaryDetach", instance, null,
                    null);
        }

        void dispatchFinishTemporaryDetach(View instance) {
            invoke(View.class, "dispatchFinishTemporaryDetach", instance, null,
                    null);
        }

        void invalidateParentCaches(View instance) {
            invoke(View.class, "invalidateParentCaches", instance, null, null);
        }

        void invalidateParentIfNeeded(View instance) {
            invoke(View.class, "invalidateParentIfNeeded", instance, null, null);
        }

        boolean performButtonActionOnTouchDown(View instance, MotionEvent event) {
            return (Boolean) invoke(View.class,
                    "performButtonActionOnTouchDown", instance,
                    new Class<?>[] { MotionEvent.class },
                    new Object[] { event });
        }

        float getHorizontalScrollFactor(View instance) {
            return (Float) invoke(View.class, "getHorizontalScrollFactor",
                    instance, null, null);
        }

        void offsetLeftAndRight(View view, int offset) {
            invoke(View.class, "offsetLeftAndRight", view,
                    new Class<?>[] { Integer.class }, new Object[] { offset });
        }

        boolean isScrollingInDirection(OverScroller scroller, float xvel,
                float yvel) {
            return (Boolean) invoke(OverScroller.class,
                    "isScrollingInDirection", scroller, new Class<?>[] {
                            Float.class, Float.class }, new Object[] { xvel,
                            yvel });
        }
    }

    public AbsHorizontalListView(Context context) {
        super(context);
        initAbsListView();

        setHorizontalScrollBarEnabled(true);
        final int[] stylableView = new int[50]; // R.styleable.View
        for (int i = 0; i < 60; i++) {
            stylableView[i] = i;
        }
        TypedArray a = context.obtainStyledAttributes(stylableView);
        initializeScrollbars(a);
        a.recycle();
    }

    public AbsHorizontalListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsHorizontalListView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        initAbsListView();
        final int[] stylableAbsListView = new int[] // R.styleable.AbsListView
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        TypedArray a = context.obtainStyledAttributes(attrs,
                stylableAbsListView, defStyle, 0);

        Drawable d = a.getDrawable(0x00000000); // R.styleable.AbsListView_listSelector
        if (d != null) {
            setSelector(d);
        }

        mDrawSelectorOnLeft = false;

        boolean scrollingCacheEnabled = a.getBoolean(3, true); // R.styleable.AbsListView_scrollingCache
        setScrollingCacheEnabled(scrollingCacheEnabled);

        int transcriptMode = a.getInt(5, // R.styleable.AbsListView_transcriptMode
                TRANSCRIPT_MODE_DISABLED);
        setTranscriptMode(transcriptMode);

        int color = a.getColor(6, 0); // R.styleable.AbsListView_cacheColorHint
        setCacheColorHint(color);

        boolean smoothScrollbar = a.getBoolean(9, true); // R.styleable.AbsListView_smoothScrollbar
        setSmoothScrollbarEnabled(smoothScrollbar);

        setChoiceMode(a.getInt(0x00000007, // R.styleable.AbsListView_choiceMode
                CHOICE_MODE_NONE));

        a.recycle();
    }

    private void initAbsListView() {
        // Setting focusable in touch mode will set the focusable property to
        // true
        setClickable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        setAlwaysDrawnWithCacheEnabled(false);
        setScrollingCacheEnabled(true);

        final ViewConfiguration configuration = ViewConfiguration
                .get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mOverflingDistance = configuration.getScaledOverflingDistance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAdapter(ListAdapter adapter) {
        if (adapter != null) {
            if (mChoiceMode != CHOICE_MODE_NONE && mAdapterHasStableIds
                    && mCheckedIdStates == null) {
                mCheckedIdStates = new HashMap<Long, Integer>();
            }
        }

        if (mCheckStates != null) {
            mCheckStates.clear();
        }

        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
    }

    /**
     * Returns the number of items currently selected. This will only be valid
     * if the choice mode is not {@link #CHOICE_MODE_NONE} (default).
     * 
     * <p>
     * To determine the specific items that are currently selected, use one of
     * the <code>getChecked*</code> methods.
     * 
     * @return The number of items currently selected
     * 
     * @see #getCheckedItemPosition()
     * @see #getCheckedItemPositions()
     * @see #getCheckedItemIds()
     */
    public int getCheckedItemCount() {
        return mCheckedItemCount;
    }

    /**
     * Returns the checked state of the specified position. The result is only
     * valid if the choice mode has been set to {@link #CHOICE_MODE_SINGLE} or
     * {@link #CHOICE_MODE_MULTIPLE}.
     * 
     * @param position
     *            The item whose checked state to return
     * @return The item's checked state or <code>false</code> if choice mode is
     *         invalid
     * 
     * @see #setChoiceMode(int)
     */
    public boolean isItemChecked(int position) {
        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
            return mCheckStates.get(position);
        }

        return false;
    }

    /**
     * Returns the currently checked item. The result is only valid if the
     * choice mode has been set to {@link #CHOICE_MODE_SINGLE}.
     * 
     * @return The position of the currently checked item or
     *         {@link #INVALID_POSITION} if nothing is selected
     * 
     * @see #setChoiceMode(int)
     */
    public int getCheckedItemPosition() {
        if (mChoiceMode == CHOICE_MODE_SINGLE && mCheckStates != null
                && mCheckStates.size() == 1) {
            return mCheckStates.keyAt(0);
        }

        return INVALID_POSITION;
    }

    /**
     * Returns the set of checked items in the list. The result is only valid if
     * the choice mode has not been set to {@link #CHOICE_MODE_NONE}.
     * 
     * @return A SparseBooleanArray which will return true for each call to
     *         get(int position) where position is a position in the list, or
     *         <code>null</code> if the choice mode is set to
     *         {@link #CHOICE_MODE_NONE}.
     */
    public SparseBooleanArray getCheckedItemPositions() {
        if (mChoiceMode != CHOICE_MODE_NONE) {
            return mCheckStates;
        }
        return null;
    }

    /**
     * Returns the set of checked items ids. The result is only valid if the
     * choice mode has not been set to {@link #CHOICE_MODE_NONE} and the adapter
     * has stable IDs. ({@link ListAdapter#hasStableIds()} == {@code true})
     * 
     * @return A new array which contains the id of each checked item in the
     *         list.
     */
    public long[] getCheckedItemIds() {
        if (mChoiceMode == CHOICE_MODE_NONE || mCheckedIdStates == null
                || mAdapter == null) {
            return new long[0];
        }

        final HashMap<Long, Integer> idStates = mCheckedIdStates;
        final int count = idStates.size();
        final long[] ids = new long[count];

        final Set<Long> keys = idStates.keySet();
        int i = 0;
        for (Long key : keys) {
            ids[i] = key;
            i++;
        }

        return ids;
    }

    /**
     * Clear any choices previously set
     */
    public void clearChoices() {
        if (mCheckStates != null) {
            mCheckStates.clear();
        }
        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
        mCheckedItemCount = 0;
    }

    /**
     * Sets the checked state of the specified position. The is only valid if
     * the choice mode has been set to {@link #CHOICE_MODE_SINGLE} or
     * {@link #CHOICE_MODE_MULTIPLE}.
     * 
     * @param position
     *            The item whose checked state is to be checked
     * @param value
     *            The new checked state for the item
     */
    public void setItemChecked(int position, boolean value) {
        if (mChoiceMode == CHOICE_MODE_NONE) {
            return;
        }

        // Start selection mode if needed. We don't need to if we're unchecking
        // something.
        if (value && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL
                && mChoiceActionMode == null) {
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        if (mChoiceMode == CHOICE_MODE_MULTIPLE
                || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
            boolean oldValue = mCheckStates.get(position);
            mCheckStates.put(position, value);
            if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                if (value) {
                    mCheckedIdStates
                            .put(mAdapter.getItemId(position), position);
                } else {
                    mCheckedIdStates.remove(mAdapter.getItemId(position));
                }
            }
            if (oldValue != value) {
                if (value) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
            }
            if (mChoiceActionMode != null) {
                final long id = mAdapter.getItemId(position);
                mMultiChoiceModeCallback.onItemCheckedStateChanged(
                        mChoiceActionMode, position, id, value);
            }
        } else {
            boolean updateIds = mCheckedIdStates != null
                    && mAdapter.hasStableIds();
            // Clear all values if we're checking something, or unchecking the
            // currently
            // selected item
            if (value || isItemChecked(position)) {
                mCheckStates.clear();
                if (updateIds) {
                    mCheckedIdStates.clear();
                }
            }
            // this may end up selecting the value we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact
            // getCheckedItemPosition relies on
            if (value) {
                mCheckStates.put(position, true);
                if (updateIds) {
                    mCheckedIdStates
                            .put(mAdapter.getItemId(position), position);
                }
                mCheckedItemCount = 1;
            } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                mCheckedItemCount = 0;
            }
        }

        // Do not generate a data change while we are in the layout phase
        if (!mInLayout && !mBlockLayoutRequests) {
            mDataChanged = true;
            rememberSyncState();
            requestLayout();
        }
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        boolean handled = false;
        boolean dispatchItemClick = true;

        if (mChoiceMode != CHOICE_MODE_NONE) {
            handled = true;
            boolean checkedStateChanged = false;

            if (mChoiceMode == CHOICE_MODE_MULTIPLE
                    || (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode != null)) {
                boolean newValue = !mCheckStates.get(position, false);
                mCheckStates.put(position, newValue);
                if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                    if (newValue) {
                        mCheckedIdStates.put(mAdapter.getItemId(position),
                                position);
                    } else {
                        mCheckedIdStates.remove(mAdapter.getItemId(position));
                    }
                }
                if (newValue) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
                if (mChoiceActionMode != null) {
                    mMultiChoiceModeCallback.onItemCheckedStateChanged(
                            mChoiceActionMode, position, id, newValue);
                    dispatchItemClick = false;
                }
                checkedStateChanged = true;
            } else if (mChoiceMode == CHOICE_MODE_SINGLE) {
                boolean newValue = !mCheckStates.get(position, false);
                if (newValue) {
                    mCheckStates.clear();
                    mCheckStates.put(position, true);
                    if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                        mCheckedIdStates.clear();
                        mCheckedIdStates.put(mAdapter.getItemId(position),
                                position);
                    }
                    mCheckedItemCount = 1;
                } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                    mCheckedItemCount = 0;
                }
                checkedStateChanged = true;
            }

            if (checkedStateChanged) {
                updateOnScreenCheckedViews();
            }
        }

        if (dispatchItemClick) {
            handled |= super.performItemClick(view, position, id);
        }

        return handled;
    }

    /**
     * Perform a quick, in-place update of the checked or activated state on all
     * visible item views. This should only be called when a valid choice mode
     * is active.
     */
    private void updateOnScreenCheckedViews() {
        final int firstPos = mFirstPosition;
        final int count = getChildCount();
        final boolean useActivated = getContext().getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.HONEYCOMB;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int position = firstPos + i;

            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(mCheckStates.get(position));
            } else if (useActivated) {
                child.setActivated(mCheckStates.get(position));
            }
        }
    }

    /**
     * @see #setChoiceMode(int)
     * 
     * @return The current choice mode
     */
    public int getChoiceMode() {
        return mChoiceMode;
    }

    /**
     * Defines the choice behavior for the List. By default, Lists do not have
     * any choice behavior ({@link #CHOICE_MODE_NONE}). By setting the
     * choiceMode to {@link #CHOICE_MODE_SINGLE}, the List allows up to one item
     * to be in a chosen state. By setting the choiceMode to
     * {@link #CHOICE_MODE_MULTIPLE}, the list allows any number of items to be
     * chosen.
     * 
     * @param choiceMode
     *            One of {@link #CHOICE_MODE_NONE}, {@link #CHOICE_MODE_SINGLE},
     *            or {@link #CHOICE_MODE_MULTIPLE}
     */
    public void setChoiceMode(int choiceMode) {
        mChoiceMode = choiceMode;
        if (mChoiceActionMode != null) {
            mChoiceActionMode.finish();
            mChoiceActionMode = null;
        }
        if (mChoiceMode != CHOICE_MODE_NONE) {
            if (mCheckStates == null) {
                mCheckStates = new SparseBooleanArray();
            }
            if (mCheckedIdStates == null && mAdapter != null
                    && mAdapter.hasStableIds()) {
                mCheckedIdStates = new HashMap<Long, Integer>();
            }
            // Modal multi-choice mode only has choices when the mode is active.
            // Clear them.
            if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
                clearChoices();
                setLongClickable(true);
            }
        }
    }

    /**
     * Set a {@link MultiChoiceModeListener} that will manage the lifecycle of
     * the selection {@link ActionMode}. Only used when the choice mode is set
     * to {@link #CHOICE_MODE_MULTIPLE_MODAL}.
     * 
     * @param listener
     *            Listener that will manage the selection mode
     * 
     * @see #setChoiceMode(int)
     */
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        if (mMultiChoiceModeCallback == null) {
            mMultiChoiceModeCallback = new MultiChoiceModeWrapper();
        }
        mMultiChoiceModeCallback.setWrapped(listener);
    }

    /**
     * @return true if all list content currently fits within the view
     *         boundaries
     */
    private boolean contentFits() {
        final int childCount = getChildCount();
        if (childCount == 0)
            return true;
        if (childCount != mItemCount)
            return false;

        return getChildAt(0).getTop() >= mListPadding.top
                && getChildAt(childCount - 1).getBottom() <= getHeight()
                        - mListPadding.bottom;
    }

    /**
     * When smooth scrollbar is enabled, the position and size of the scrollbar
     * thumb is computed based on the number of visible pixels in the visible
     * items. This however assumes that all list items have the same height. If
     * you use a list in which items have different heights, the scrollbar will
     * change appearance as the user scrolls through the list. To avoid this
     * issue, you need to disable this property.
     * 
     * When smooth scrollbar is disabled, the position and size of the scrollbar
     * thumb is based solely on the number of items in the adapter and the
     * position of the visible items inside the adapter. This provides a stable
     * scrollbar as the user navigates through a list of items with varying
     * heights.
     * 
     * @param enabled
     *            Whether or not to enable smooth scrollbar.
     * 
     * @see #setSmoothScrollbarEnabled(boolean)
     * @attr ref android.R.styleable#AbsListView_smoothScrollbar
     */
    public void setSmoothScrollbarEnabled(boolean enabled) {
        mSmoothScrollbarEnabled = enabled;
    }

    /**
     * Returns the current state of the fast scroll feature.
     * 
     * @return True if smooth scrollbar is enabled is enabled, false otherwise.
     * 
     * @see #setSmoothScrollbarEnabled(boolean)
     */
    @ViewDebug.ExportedProperty
    public boolean isSmoothScrollbarEnabled() {
        return mSmoothScrollbarEnabled;
    }

    /**
     * Set the listener that will receive notifications every time the list
     * scrolls.
     * 
     * @param l
     *            the scroll listener
     */
    public void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
        invokeOnItemScrollListener();
    }

    /**
     * Notify our scroll listener (if there is one) of a change in scroll state
     */
    void invokeOnItemScrollListener() {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(),
                    mItemCount);
        }
        // dummy values, View's implementation does not use these.
        onScrollChanged(0, 0, 0, 0);
    }

    /**
     * Indicates whether the children's drawing cache is used during a scroll.
     * By default, the drawing cache is enabled but this will consume more
     * memory.
     * 
     * @return true if the scrolling cache is enabled, false otherwise
     * 
     * @see #setScrollingCacheEnabled(boolean)
     * @see View#setDrawingCacheEnabled(boolean)
     */
    @ViewDebug.ExportedProperty
    public boolean isScrollingCacheEnabled() {
        return mScrollingCacheEnabled;
    }

    /**
     * Enables or disables the children's drawing cache during a scroll. By
     * default, the drawing cache is enabled but this will use more memory.
     * 
     * When the scrolling cache is enabled, the caches are kept after the first
     * scrolling. You can manually clear the cache by calling
     * {@link android.view.ViewGroup#setChildrenDrawingCacheEnabled(boolean)}.
     * 
     * @param enabled
     *            true to enable the scroll cache, false otherwise
     * 
     * @see #isScrollingCacheEnabled()
     * @see View#setDrawingCacheEnabled(boolean)
     */
    public void setScrollingCacheEnabled(boolean enabled) {
        if (mScrollingCacheEnabled && !enabled) {
            clearScrollingCache();
        }
        mScrollingCacheEnabled = enabled;
    }

    /**
     * Enables or disables the type filter window. If enabled, typing when this
     * view has focus will filter the children to match the users input. Note
     * that the {@link Adapter} used by this view must implement the
     * {@link Filterable} interface.
     * 
     * @param textFilterEnabled
     *            true to enable type filtering, false otherwise
     * 
     * @see Filterable
     */
    public void setTextFilterEnabled(boolean textFilterEnabled) {
        mTextFilterEnabled = textFilterEnabled;
    }

    /**
     * Indicates whether type filtering is enabled for this view
     * 
     * @return true if type filtering is enabled, false otherwise
     * 
     * @see #setTextFilterEnabled(boolean)
     * @see Filterable
     */
    @ViewDebug.ExportedProperty
    public boolean isTextFilterEnabled() {
        return mTextFilterEnabled;
    }

    @Override
    public void getFocusedRect(Rect r) {
        View view = getSelectedView();
        if (view != null && view.getParent() == this) {
            // the focused rectangle of the selected view offset into the
            // coordinate space of this view.
            view.getFocusedRect(r);
            offsetDescendantRectToMyCoords(view, r);
        } else {
            // otherwise, just the norm
            super.getFocusedRect(r);
        }
    }

    private void useDefaultSelector() {
        setSelector(getResources().getDrawable(
                R.drawable.list_selector_background));
    }

    void requestLayoutIfNecessary() {
        if (getChildCount() > 0) {
            resetList();
            requestLayout();
            invalidate();
        }
    }

    static class SavedState extends BaseSavedState {
        long selectedId;
        long firstId;
        int viewLeft;
        int position;
        int width;
        String filter;
        boolean inActionMode;
        int checkedItemCount;
        SparseBooleanArray checkState;
        HashMap<Long, Integer> checkIdState;

        /**
         * Constructor called from
         * {@link AbsHorizontalListView#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            selectedId = in.readLong();
            firstId = in.readLong();
            viewLeft = in.readInt();
            position = in.readInt();
            width = in.readInt();
            filter = in.readString();
            inActionMode = in.readByte() != 0;
            checkedItemCount = in.readInt();
            checkState = in.readSparseBooleanArray();
            final int N = in.readInt();
            if (N > 0) {
                checkIdState = new HashMap<Long, Integer>();
                for (int i = 0; i < N; i++) {
                    final long key = in.readLong();
                    final int value = in.readInt();
                    checkIdState.put(key, value);
                }
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(selectedId);
            out.writeLong(firstId);
            out.writeInt(viewLeft);
            out.writeInt(position);
            out.writeInt(width);
            out.writeString(filter);
            out.writeByte((byte) (inActionMode ? 1 : 0));
            out.writeInt(checkedItemCount);
            out.writeSparseBooleanArray(checkState);
            final int N = checkIdState != null ? checkIdState.size() : 0;
            out.writeInt(N);
            for (Map.Entry<Long, Integer> entry : checkIdState.entrySet()) {
                out.writeLong(entry.getKey());
                out.writeInt(entry.getValue());
            }
        }

        @Override
        public String toString() {
            return "AbsHorizontalListView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " selectedId=" + selectedId + " firstId=" + firstId
                    + " viewLeft=" + viewLeft + " position=" + position
                    + " width=" + width + " filter=" + filter + " checkState="
                    + checkState + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {

        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        boolean haveChildren = getChildCount() > 0 && mItemCount > 0;
        long selectedId = getSelectedItemId();
        ss.selectedId = selectedId;
        ss.width = getWidth();

        if (selectedId >= 0) {
            // Remember the selection
            ss.viewLeft = mSelectedLeft;
            ss.position = getSelectedItemPosition();
            ss.firstId = INVALID_POSITION;
        } else {
            if (haveChildren && mFirstPosition > 0) {
                // Remember the position of the first child.
                // We only do this if we are not currently at the top of
                // the list, for two reasons:
                // (1) The list may be in the process of becoming empty, in
                // which case mItemCount may not be 0, but if we try to
                // ask for any information about position 0 we will crash.
                // (2) Being "at the top" seems like a special case, anyway,
                // and the user wouldn't expect to end up somewhere else when
                // they revisit the list even if its content has changed.
                View v = getChildAt(0);
                ss.viewLeft = v.getLeft();
                int firstPos = mFirstPosition;
                if (firstPos >= mItemCount) {
                    firstPos = mItemCount - 1;
                }
                ss.position = firstPos;
                ss.firstId = mAdapter.getItemId(firstPos);
            } else {
                ss.viewLeft = 0;
                ss.firstId = INVALID_POSITION;
                ss.position = 0;
            }
        }

        ss.filter = null;
        if (mFiltered) {
            final EditText textFilter = mTextFilter;
            if (textFilter != null) {
                Editable filterText = textFilter.getText();
                if (filterText != null) {
                    ss.filter = filterText.toString();
                }
            }
        }

        ss.inActionMode = mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL
                && mChoiceActionMode != null;

        if (mCheckStates != null) {
            ss.checkState = mCheckStates.clone();
        }
        if (mCheckedIdStates != null) {
            ss.checkIdState = new HashMap<Long, Integer>(mCheckedIdStates);
        }
        ss.checkedItemCount = mCheckedItemCount;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        mDataChanged = true;

        mSyncHeight = ss.width;

        if (ss.selectedId >= 0) {
            mNeedSync = true;
            mSyncColId = ss.selectedId;
            mSyncPosition = ss.position;
            mSpecificLeft = ss.viewLeft;
            mSyncMode = SYNC_SELECTED_POSITION;
        } else if (ss.firstId >= 0) {
            setSelectedPositionInt(INVALID_POSITION);
            // Do this before setting mNeedSync since setNextSelectedPosition
            // looks at mNeedSync
            setNextSelectedPositionInt(INVALID_POSITION);
            mSelectorPosition = INVALID_POSITION;
            mNeedSync = true;
            mSyncColId = ss.firstId;
            mSyncPosition = ss.position;
            mSpecificLeft = ss.viewLeft;
            mSyncMode = SYNC_FIRST_POSITION;
        }

        if (ss.checkState != null) {
            mCheckStates = ss.checkState;
        }

        if (ss.checkIdState != null) {
            mCheckedIdStates = ss.checkIdState;
        }

        mCheckedItemCount = ss.checkedItemCount;

        if (ss.inActionMode && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL
                && mMultiChoiceModeCallback != null) {
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        requestLayout();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
            Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus && mSelectedPosition < 0 && !isInTouchMode()) {
            if (!mIsAttached && mAdapter != null) {
                // Data may have changed while we were detached and it's valid
                // to change focus while detached. Refresh so we don't die.
                mDataChanged = true;
                mOldItemCount = mItemCount;
                mItemCount = mAdapter.getCount();
            }
            resurrectSelection();
        }
    }

    @Override
    public void requestLayout() {
        if (!mBlockLayoutRequests && !mInLayout) {
            super.requestLayout();
        }
    }

    /**
     * The list is empty. Clear everything out.
     */
    void resetList() {
        removeAllViewsInLayout();
        mFirstPosition = 0;
        mDataChanged = false;
        mPositionScrollAfterLayout = null;
        mNeedSync = false;
        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedColId = INVALID_COL_ID;
        setSelectedPositionInt(INVALID_POSITION);
        setNextSelectedPositionInt(INVALID_POSITION);
        mSelectedLeft = 0;
        mSelectorPosition = INVALID_POSITION;
        mSelectorRect.setEmpty();
        invalidate();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        final int count = getChildCount();
        if (count > 0) {
            if (mSmoothScrollbarEnabled) {
                int extent = count * 100;

                View view = getChildAt(0);
                final int left = view.getLeft();
                int width = view.getWidth();
                if (width > 0) {
                    extent += (left * 100) / width;
                }

                view = getChildAt(count - 1);
                final int right = view.getRight();
                width = view.getWidth();
                if (width > 0) {
                    extent -= ((right - getWidth()) * 100) / width;
                }

                return extent;
            } else {
                return 1;
            }
        }
        return 0;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        final int firstPosition = mFirstPosition;
        final int childCount = getChildCount();
        if (firstPosition >= 0 && childCount > 0) {
            if (mSmoothScrollbarEnabled) {
                final View view = getChildAt(0);
                final int left = view.getLeft();
                int width = view.getWidth();
                if (width > 0) {
                    return Math.max(firstPosition
                            * 100
                            - (left * 100)
                            / width
                            + (int) ((float) getScrollX() / getWidth()
                                    * mItemCount * 100), 0);
                }
            } else {
                int index;
                final int count = mItemCount;
                if (firstPosition == 0) {
                    index = 0;
                } else if (firstPosition + childCount == count) {
                    index = count;
                } else {
                    index = firstPosition + childCount / 2;
                }
                return (int) (firstPosition + childCount
                        * (index / (float) count));
            }
        }
        return 0;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        int result;
        if (mSmoothScrollbarEnabled) {
            result = Math.max(mItemCount * 100, 0);
            if (getScrollX() != 0) {
                // Compensate for overscroll
                result += Math.abs((int) ((float) getScrollX() / getWidth()
                        * mItemCount * 100));
            }
        } else {
            result = mItemCount;
        }
        return result;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        final int count = getChildCount();
        final float fadeEdge = super.getLeftFadingEdgeStrength();
        if (count == 0) {
            return fadeEdge;
        } else {
            if (mFirstPosition > 0) {
                return 1.0f;
            }

            final int left = getChildAt(0).getLeft();
            final float fadeLength = (float) getHorizontalFadingEdgeLength();
            return left < getPaddingLeft() ? (float) -(left - getPaddingLeft())
                    / fadeLength : fadeEdge;
        }
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        final int count = getChildCount();
        final float fadeEdge = super.getRightFadingEdgeStrength();
        if (count == 0) {
            return fadeEdge;
        } else {
            if (mFirstPosition + count - 1 < mItemCount - 1) {
                return 1.0f;
            }

            final int right = getChildAt(count - 1).getRight();
            final int width = getWidth();
            final float fadeLength = (float) getHorizontalFadingEdgeLength();
            return right > width - getPaddingRight() ? (float) (right - width + getPaddingRight())
                    / fadeLength
                    : fadeEdge;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSelector == null) {
            useDefaultSelector();
        }
        final Rect listPadding = mListPadding;
        listPadding.left = mSelectionLeftPadding + getPaddingLeft();
        listPadding.top = mSelectionTopPadding + getPaddingTop();
        listPadding.right = mSelectionRightPadding + getPaddingRight();
        listPadding.bottom = mSelectionBottomPadding + getPaddingBottom();

        // Check if our previous measured size was at a point where we should
        // scroll later.
        if (mTranscriptMode == TRANSCRIPT_MODE_NORMAL) {
            final int childCount = getChildCount();
            final int listBottom = getHeight() - getPaddingBottom();
            final View lastChild = getChildAt(childCount - 1);
            final int lastBottom = lastChild != null ? lastChild.getBottom()
                    : listBottom;
            mForceTranscriptScroll = mFirstPosition + childCount >= mLastHandledItemCount
                    && lastBottom <= listBottom;
        }
    }

    /**
     * Subclasses should NOT override this method but {@link #layoutChildren()}
     * instead.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mInLayout = true;
        if (changed) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).forceLayout();
            }
            mRecycler.markChildrenDirty();
        }

        // if (mFastScroller != null && mItemCount != mOldItemCount) {
        // mFastScroller.onItemCountChanged(mOldItemCount, mItemCount);
        // }

        layoutChildren();
        mInLayout = false;

        mOverscrollMax = (r - l) / OVERSCROLL_LIMIT_DIVISOR;
    }

    /**
     * Subclasses must override this method to layout their children.
     */
    protected void layoutChildren() {
    }

    void updateScrollIndicators() {
        if (mScrollLeft != null) {
            boolean canScrollLeft;
            // 0th element is not visible
            canScrollLeft = mFirstPosition > 0;

            // ... Or top of 0th element is not visible
            if (!canScrollLeft) {
                if (getChildCount() > 0) {
                    View child = getChildAt(0);
                    canScrollLeft = child.getLeft() < mListPadding.left;
                }
            }

            mScrollLeft.setVisibility(canScrollLeft ? View.VISIBLE
                    : View.INVISIBLE);
        }

        if (mScrollRight != null) {
            boolean canScrollRight;
            int count = getChildCount();

            // Last item is not visible
            canScrollRight = (mFirstPosition + count) < mItemCount;

            // ... Or bottom of the last element is not visible
            if (!canScrollRight && count > 0) {
                View child = getChildAt(count - 1);
                canScrollRight = child.getRight() > getRight()
                        - mListPadding.right;
            }

            mScrollRight.setVisibility(canScrollRight ? View.VISIBLE
                    : View.INVISIBLE);
        }
    }

    @Override
    @ViewDebug.ExportedProperty
    public View getSelectedView() {
        if (mItemCount > 0 && mSelectedPosition >= 0) {
            return getChildAt(mSelectedPosition - mFirstPosition);
        } else {
            return null;
        }
    }

    /**
     * List padding is the maximum of the normal view's padding and the padding
     * of the selector.
     * 
     * @see android.view.View#getPaddingTop()
     * @see #getSelector()
     * 
     * @return The top list padding.
     */
    public int getListPaddingTop() {
        return mListPadding.top;
    }

    /**
     * List padding is the maximum of the normal view's padding and the padding
     * of the selector.
     * 
     * @see android.view.View#getPaddingBottom()
     * @see #getSelector()
     * 
     * @return The bottom list padding.
     */
    public int getListPaddingBottom() {
        return mListPadding.bottom;
    }

    /**
     * List padding is the maximum of the normal view's padding and the padding
     * of the selector.
     * 
     * @see android.view.View#getPaddingLeft()
     * @see #getSelector()
     * 
     * @return The left list padding.
     */
    public int getListPaddingLeft() {
        return mListPadding.left;
    }

    /**
     * List padding is the maximum of the normal view's padding and the padding
     * of the selector.
     * 
     * @see android.view.View#getPaddingRight()
     * @see #getSelector()
     * 
     * @return The right list padding.
     */
    public int getListPaddingRight() {
        return mListPadding.right;
    }

    /**
     * Get a view and have it show the data associated with the specified
     * position. This is called when we have already discovered that the view is
     * not available for reuse in the recycle bin. The only choices left are
     * converting an old view or making a new one.
     * 
     * @param position
     *            The position to display
     * @param isScrap
     *            Array of at least 1 boolean, the first entry will become true
     *            if the returned view was taken from the scrap heap, false if
     *            otherwise.
     * 
     * @return A view displaying the data associated with the specified position
     */
    View obtainView(int position, boolean[] isScrap) {
        isScrap[0] = false;
        View scrapView;

        scrapView = mRecycler.getTransientStateView(position);
        if (scrapView != null) {
            return scrapView;
        }

        scrapView = mRecycler.getScrapView(position);

        View child;
        if (scrapView != null) {
            child = mAdapter.getView(position, scrapView, this);

            // if (child.getImportantForAccessibility() ==
            // IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            // child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            // }

            if (child != scrapView) {
                mRecycler.addScrapView(scrapView, position);
                if (mCacheColorHint != 0) {
                    child.setDrawingCacheBackgroundColor(mCacheColorHint);
                }
            } else {
                isScrap[0] = true;
                mHideApiProxy.dispatchFinishTemporaryDetach(child);
            }
        } else {
            child = mAdapter.getView(position, null, this);

            // if (child.getImportantForAccessibility() ==
            // IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            // child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            // }

            if (mCacheColorHint != 0) {
                child.setDrawingCacheBackgroundColor(mCacheColorHint);
            }
        }

        if (mAdapterHasStableIds) {
            final ViewGroup.LayoutParams vlp = child.getLayoutParams();
            LayoutParams lp;
            if (vlp == null) {
                lp = (LayoutParams) generateDefaultLayoutParams();
            } else if (!checkLayoutParams(vlp)) {
                lp = (LayoutParams) generateLayoutParams(vlp);
            } else {
                lp = (LayoutParams) vlp;
            }
            lp.itemId = mAdapter.getItemId(position);
            child.setLayoutParams(lp);
        }

        return child;
    }

    void positionSelector(int position, View sel) {
        if (position != INVALID_POSITION) {
            mSelectorPosition = position;
        }

        final Rect selectorRect = mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(),
                sel.getBottom());
        if (sel instanceof SelectionBoundsAdjuster) {
            ((SelectionBoundsAdjuster) sel)
                    .adjustListItemSelectionBounds(selectorRect);
        }
        positionSelector(selectorRect.left, selectorRect.top,
                selectorRect.right, selectorRect.bottom);

        final boolean isChildViewEnabled = mIsChildViewEnabled;
        if (sel.isEnabled() != isChildViewEnabled) {
            mIsChildViewEnabled = !isChildViewEnabled;
            if (getSelectedItemPosition() != INVALID_POSITION) {
                refreshDrawableState();
            }
        }
    }

    private void positionSelector(int l, int t, int r, int b) {
        mSelectorRect.set(l - mSelectionLeftPadding, t - mSelectionTopPadding,
                r + mSelectionRightPadding, b + mSelectionBottomPadding);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int saveCount = 0;
        final boolean clipToPadding = (mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK;
        if (clipToPadding) {
            saveCount = canvas.save();
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.clipRect(scrollX + getPaddingLeft(), scrollY
                    + getPaddingTop(), scrollX + getRight() - getLeft()
                    - getPaddingRight(), scrollY + getBottom() - getTop()
                    - getPaddingBottom());
            mHideApiProxy.mGroupFlags(this, mHideApiProxy.mGroupFlags(this)
                    & ~CLIP_TO_PADDING_MASK);
        }

        final boolean drawSelectorOnLeft = mDrawSelectorOnLeft;
        if (!drawSelectorOnLeft) {
            drawSelector(canvas);
        }

        super.dispatchDraw(canvas);

        if (drawSelectorOnLeft) {
            drawSelector(canvas);
        }

        if (clipToPadding) {
            canvas.restoreToCount(saveCount);
            mHideApiProxy.mGroupFlags(this, mHideApiProxy.mGroupFlags(this)
                    | CLIP_TO_PADDING_MASK);
        }
    }

    @Override
    protected boolean isPaddingOffsetRequired() {
        return (mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) != CLIP_TO_PADDING_MASK;
    }

    @Override
    protected int getLeftPaddingOffset() {
        return (mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK ? 0
                : -getPaddingLeft();
    }

    @Override
    protected int getTopPaddingOffset() {
        return (mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK ? 0
                : -getPaddingTop();
    }

    @Override
    protected int getRightPaddingOffset() {
        return (mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK ? 0
                : getPaddingRight();
    }

    @Override
    protected int getBottomPaddingOffset() {
        return (mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK ? 0
                : getPaddingBottom();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (getChildCount() > 0) {
            mDataChanged = true;
            rememberSyncState();
        }
    }

    /**
     * @return True if the current touch mode requires that we draw the selector
     *         in the pressed state.
     */
    boolean touchModeDrawsInPressedState() {
        // FIXME use isPressed for this
        switch (mTouchMode) {
        case TOUCH_MODE_TAP:
        case TOUCH_MODE_DONE_WAITING:
            return true;
        default:
            return false;
        }
    }

    /**
     * Indicates whether this view is in a state where the selector should be
     * drawn. This will happen if we have focus but are not in touch mode, or we
     * are in the middle of displaying the pressed state for an item.
     * 
     * @return True if the selector should be shown
     */
    boolean shouldShowSelector() {
        return (hasFocus() && !isInTouchMode())
                || touchModeDrawsInPressedState();
    }

    private void drawSelector(Canvas canvas) {
        if (!mSelectorRect.isEmpty()) {
            final Drawable selector = mSelector;
            selector.setBounds(mSelectorRect);
            selector.draw(canvas);
        }
    }

    /**
     * Controls whether the selection highlight drawable should be drawn on left
     * of the item or behind it.
     * 
     * @param onLeft
     *            If true, the selector will be drawn on the item it is
     *            highlighting. The default is false.
     * 
     * @attr ref android.R.styleable#AbsListView_drawSelectorOnTop
     */
    public void setDrawSelectorOnLeft(boolean onLeft) {
        mDrawSelectorOnLeft = onLeft;
    }

    /**
     * Set a Drawable that should be used to highlight the currently selected
     * item.
     * 
     * @param resID
     *            A Drawable resource to use as the selection highlight.
     * 
     * @attr ref android.R.styleable#AbsListView_listSelector
     */
    public void setSelector(int resID) {
        setSelector(getResources().getDrawable(resID));
    }

    public void setSelector(Drawable sel) {
        if (mSelector != null) {
            mSelector.setCallback(null);
            unscheduleDrawable(mSelector);
        }
        mSelector = sel;
        Rect padding = new Rect();
        sel.getPadding(padding);
        mSelectionLeftPadding = padding.left;
        mSelectionTopPadding = padding.top;
        mSelectionRightPadding = padding.right;
        mSelectionBottomPadding = padding.bottom;
        sel.setCallback(this);
        updateSelectorState();
    }

    /**
     * Returns the selector {@link android.graphics.drawable.Drawable} that is
     * used to draw the selection in the list.
     * 
     * @return the drawable used to display the selector
     */
    public Drawable getSelector() {
        return mSelector;
    }

    /**
     * Sets the selector state to "pressed" and posts a CheckForKeyLongPress to
     * see if this is a long press.
     */
    void keyPressed() {
        if (!isEnabled() || !isClickable()) {
            return;
        }

        Drawable selector = mSelector;
        Rect selectorRect = mSelectorRect;
        if (selector != null && (isFocused() || touchModeDrawsInPressedState())
                && !selectorRect.isEmpty()) {

            final View v = getChildAt(mSelectedPosition - mFirstPosition);

            if (v != null) {
                if (v.hasFocusable())
                    return;
                v.setPressed(true);
            }
            setPressed(true);

            final boolean longClickable = isLongClickable();
            Drawable d = selector.getCurrent();
            if (d != null && d instanceof TransitionDrawable) {
                if (longClickable) {
                    ((TransitionDrawable) d).startTransition(ViewConfiguration
                            .getLongPressTimeout());
                } else {
                    ((TransitionDrawable) d).resetTransition();
                }
            }
            if (longClickable && !mDataChanged) {
                if (mPendingCheckForKeyLongPress == null) {
                    mPendingCheckForKeyLongPress = new CheckForKeyLongPress();
                }
                mPendingCheckForKeyLongPress.rememberWindowAttachCount();
                postDelayed(mPendingCheckForKeyLongPress,
                        ViewConfiguration.getLongPressTimeout());
            }
        }
    }

    public void setScrollIndicators(View left, View right) {
        mScrollLeft = left;
        mScrollRight = right;
    }

    void updateSelectorState() {
        if (mSelector != null) {
            if (shouldShowSelector()) {
                mSelector.setState(getDrawableState());
            } else {
                mSelector.setState(StateSet.NOTHING);
            }
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateSelectorState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        // If the child view is enabled then do the default behavior.
        if (mIsChildViewEnabled) {
            // Common case
            return super.onCreateDrawableState(extraSpace);
        }

        // The selector uses this View's drawable state. The selected child view
        // is disabled, so we need to remove the enabled state from the drawable
        // states.
        final int enabledState = ENABLED_STATE_SET[0];

        // If we don't have any extra space, it will return one of the static
        // state arrays,
        // and clearing the enabled state on those arrays is a bad thing! If we
        // specify
        // we need extra space, it will create+copy into a new array that safely
        // mutable.
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        int enabledPos = -1;
        for (int i = state.length - 1; i >= 0; i--) {
            if (state[i] == enabledState) {
                enabledPos = i;
                break;
            }
        }

        // Remove the enabled state
        if (enabledPos >= 0) {
            System.arraycopy(state, enabledPos + 1, state, enabledPos,
                    state.length - enabledPos - 1);
        }

        return state;
    }

    @Override
    public boolean verifyDrawable(Drawable dr) {
        return mSelector == dr || super.verifyDrawable(dr);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mSelector != null)
            mSelector.jumpToCurrentState();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final ViewTreeObserver treeObserver = getViewTreeObserver();
        treeObserver.addOnTouchModeChangeListener(this);
        // if (mTextFilterEnabled && mPopup != null
        // && !mGlobalLayoutListenerAddedFilter) {
        // treeObserver.addOnGlobalLayoutListener(this);
        // }

        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            // Data may have changed while we were detached. Refresh.
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
        }
        mIsAttached = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Detach any view left in the scrap heap
        mRecycler.clear();

        final ViewTreeObserver treeObserver = getViewTreeObserver();
        treeObserver.removeOnTouchModeChangeListener(this);

        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }

        if (mFlingRunnable != null) {
            removeCallbacks(mFlingRunnable);
        }

        if (mPositionScroller != null) {
            mPositionScroller.stop();
        }

        if (mClearScrollingCache != null) {
            removeCallbacks(mClearScrollingCache);
        }

        if (mPerformClick != null) {
            removeCallbacks(mPerformClick);
        }

        if (mTouchModeReset != null) {
            removeCallbacks(mTouchModeReset);
            mTouchModeReset = null;
        }
        mIsAttached = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        final int touchMode = isInTouchMode() ? TOUCH_MODE_ON : TOUCH_MODE_OFF;

        if (!hasWindowFocus) {
            setChildrenDrawingCacheEnabled(false);
            if (mFlingRunnable != null) {
                removeCallbacks(mFlingRunnable);
                // let the fling runnable report it's new state which
                // should be idle
                mFlingRunnable.endFling();
                if (mPositionScroller != null) {
                    mPositionScroller.stop();
                }
                if (getScrollX() != 0) {
                    mHideApiProxy.mScrollX(this, 0);
                    mHideApiProxy.invalidateParentCaches(this);
                    invalidate();
                }
            }

            if (touchMode == TOUCH_MODE_OFF) {
                // Remember the last selected element
                mResurrectToPosition = mSelectedPosition;
            }
        } else {

            // If we changed touch mode since the last time we had focus
            if (touchMode != mLastTouchMode
                    && mLastTouchMode != TOUCH_MODE_UNKNOWN) {
                // If we come back in trackball mode, we bring the selection
                // back
                if (touchMode == TOUCH_MODE_OFF) {
                    // This will trigger a layout
                    resurrectSelection();

                    // If we come back in touch mode, then we want to hide the
                    // selector
                } else {
                    hideSelector();
                    mLayoutMode = LAYOUT_NORMAL;
                    layoutChildren();
                }
            }
        }

        mLastTouchMode = touchMode;
    }

    /**
     * Creates the ContextMenuInfo returned from {@link #getContextMenuInfo()}.
     * This methods knows the view, position and ID of the item that received
     * the long press.
     * 
     * @param view
     *            The view that received the long press.
     * @param position
     *            The position of the item that received the long press.
     * @param id
     *            The ID of the item that received the long press.
     * @return The extra information that should be returned by
     *         {@link #getContextMenuInfo()}.
     */
    ContextMenuInfo createContextMenuInfo(View view, int position, long id) {
        return new AdapterContextMenuInfo(view, position, id);
    }

    /**
     * A base class for Runnables that will check that their view is still
     * attached to the original window as when the Runnable was created.
     * 
     */
    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return hasWindowFocus()
                    && getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        int mClickMotionPosition;

        public void run() {
            // The data has changed since we posted this action in the event
            // queue,
            // bail out before bad things happen
            if (mDataChanged)
                return;

            final ListAdapter adapter = mAdapter;
            final int motionPosition = mClickMotionPosition;
            if (adapter != null && mItemCount > 0
                    && motionPosition != INVALID_POSITION
                    && motionPosition < adapter.getCount() && sameWindow()) {
                final View view = getChildAt(motionPosition - mFirstPosition);
                // If there is no view, something bad happened (the view
                // scrolled off the
                // screen, etc.) and we should cancel the click
                if (view != null) {
                    performItemClick(view, motionPosition,
                            adapter.getItemId(motionPosition));
                }
            }
        }
    }

    private class CheckForLongPress extends WindowRunnnable implements Runnable {
        public void run() {
            final int motionPosition = mMotionPosition;
            final View child = getChildAt(motionPosition - mFirstPosition);
            if (child != null) {
                final int longPressPosition = mMotionPosition;
                final long longPressId = mAdapter.getItemId(mMotionPosition);

                boolean handled = false;
                if (sameWindow() && !mDataChanged) {
                    handled = performLongPress(child, longPressPosition,
                            longPressId);
                }
                if (handled) {
                    mTouchMode = TOUCH_MODE_REST;
                    setPressed(false);
                    child.setPressed(false);
                } else {
                    mTouchMode = TOUCH_MODE_DONE_WAITING;
                }
            }
        }
    }

    private class CheckForKeyLongPress extends WindowRunnnable implements
            Runnable {
        public void run() {
            if (isPressed() && mSelectedPosition >= 0) {
                int index = mSelectedPosition - mFirstPosition;
                View v = getChildAt(index);

                if (!mDataChanged) {
                    boolean handled = false;
                    if (sameWindow()) {
                        handled = performLongPress(v, mSelectedPosition,
                                mSelectedColId);
                    }
                    if (handled) {
                        setPressed(false);
                        v.setPressed(false);
                    }
                } else {
                    setPressed(false);
                    if (v != null)
                        v.setPressed(false);
                }
            }
        }
    }

    boolean performLongPress(final View child, final int longPressPosition,
            final long longPressId) {
        // CHOICE_MODE_MULTIPLE_MODAL takes over long press.
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
            if (mChoiceActionMode == null
                    && (mChoiceActionMode = startActionMode(mMultiChoiceModeCallback)) != null) {
                setItemChecked(longPressPosition, true);
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            return true;
        }

        boolean handled = false;
        if (getOnItemLongClickListener() != null) {
            handled = getOnItemLongClickListener().onItemLongClick(
                    AbsHorizontalListView.this, child, longPressPosition,
                    longPressId);
        }
        if (!handled) {
            mContextMenuInfo = createContextMenuInfo(child, longPressPosition,
                    longPressId);
            handled = super.showContextMenuForChild(AbsHorizontalListView.this);
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }

    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    public boolean showContextMenu(float x, float y, int metaState) {
        final int position = pointToPosition((int) x, (int) y);
        if (position != INVALID_POSITION) {
            final long id = mAdapter.getItemId(position);
            View child = getChildAt(position - mFirstPosition);
            if (child != null) {
                mContextMenuInfo = createContextMenuInfo(child, position, id);
                return super
                        .showContextMenuForChild(AbsHorizontalListView.this);
            }
        }
        return super.showContextMenu();
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getPositionForView(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = mAdapter.getItemId(longPressPosition);
            boolean handled = false;

            if (getOnItemLongClickListener() != null) {
                handled = getOnItemLongClickListener().onItemLongClick(
                        AbsHorizontalListView.this, originalView,
                        longPressPosition, longPressId);
            }
            if (!handled) {
                mContextMenuInfo = createContextMenuInfo(
                        getChildAt(longPressPosition - mFirstPosition),
                        longPressPosition, longPressId);
                handled = super.showContextMenuForChild(originalView);
            }

            return handled;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
            if (!isEnabled()) {
                return true;
            }
            if (isClickable() && isPressed() && mSelectedPosition >= 0
                    && mAdapter != null
                    && mSelectedPosition < mAdapter.getCount()) {

                final View view = getChildAt(mSelectedPosition - mFirstPosition);
                if (view != null) {
                    performItemClick(view, mSelectedPosition, mSelectedColId);
                    view.setPressed(false);
                }
                setPressed(false);
                return true;
            }
            break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Don't dispatch setPressed to our children. We call setPressed on
        // ourselves to
        // get the selector in the right state, but we don't want to press each
        // child.
    }

    /**
     * Maps a point to a position in the list.
     * 
     * @param x
     *            X in local coordinate
     * @param y
     *            Y in local coordinate
     * @return The position of the item which contains the specified point, or
     *         {@link #INVALID_POSITION} if the point does not intersect an
     *         item.
     */
    public int pointToPosition(int x, int y) {
        Rect frame = mTouchFrame;
        if (frame == null) {
            mTouchFrame = new Rect();
            frame = mTouchFrame;
        }

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return mFirstPosition + i;
                }
            }
        }
        return INVALID_POSITION;
    }

    /**
     * Maps a point to a the rowId of the item which intersects that point.
     * 
     * @param x
     *            X in local coordinate
     * @param y
     *            Y in local coordinate
     * @return The rowId of the item which contains the specified point, or
     *         {@link #INVALID_COL_ID} if the point does not intersect an item.
     */
    public long pointToColId(int x, int y) {
        int position = pointToPosition(x, y);
        if (position >= 0) {
            return mAdapter.getItemId(position);
        }
        return INVALID_COL_ID;
    }

    final class CheckForTap implements Runnable {
        public void run() {
            if (mTouchMode == TOUCH_MODE_DOWN) {
                mTouchMode = TOUCH_MODE_TAP;
                final View child = getChildAt(mMotionPosition - mFirstPosition);
                if (child != null && !child.hasFocusable()) {
                    mLayoutMode = LAYOUT_NORMAL;

                    if (!mDataChanged) {
                        child.setPressed(true);
                        setPressed(true);
                        layoutChildren();
                        positionSelector(mMotionPosition, child);
                        refreshDrawableState();

                        final int longPressTimeout = ViewConfiguration
                                .getLongPressTimeout();
                        final boolean longClickable = isLongClickable();

                        if (mSelector != null) {
                            Drawable d = mSelector.getCurrent();
                            if (d != null && d instanceof TransitionDrawable) {
                                if (longClickable) {
                                    ((TransitionDrawable) d)
                                            .startTransition(longPressTimeout);
                                } else {
                                    ((TransitionDrawable) d).resetTransition();
                                }
                            }
                        }

                        if (longClickable) {
                            if (mPendingCheckForLongPress == null) {
                                mPendingCheckForLongPress = new CheckForLongPress();
                            }
                            mPendingCheckForLongPress
                                    .rememberWindowAttachCount();
                            postDelayed(mPendingCheckForLongPress,
                                    longPressTimeout);
                        } else {
                            mTouchMode = TOUCH_MODE_DONE_WAITING;
                        }
                    } else {
                        mTouchMode = TOUCH_MODE_DONE_WAITING;
                    }
                }
            }
        }
    }

    private boolean startScrollIfNeeded(int x) {
        // Check if we have moved far enough that it looks more like a
        // scroll than a tap
        final int deltaX = x - mMotionX;
        final int distance = Math.abs(deltaX);
        final boolean overscroll = getScrollX() != 0;
        if (overscroll || distance > mTouchSlop) {
            createScrollingCache();
            if (overscroll) {
                mTouchMode = TOUCH_MODE_OVERSCROLL;
                mMotionCorrection = 0;
            } else {
                mTouchMode = TOUCH_MODE_SCROLL;
                mMotionCorrection = deltaX > 0 ? mTouchSlop : -mTouchSlop;
            }
            final Handler handler = getHandler();
            // Handler should not be null unless the AbsHorizontalListView is
            // not attached
            // to a
            // window, which would make it very hard to scroll it... but the
            // monkeys
            // say it's possible.
            if (handler != null) {
                handler.removeCallbacks(mPendingCheckForLongPress);
            }
            setPressed(false);
            View motionView = getChildAt(mMotionPosition - mFirstPosition);
            if (motionView != null) {
                motionView.setPressed(false);
            }
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            // Time to start stealing events! Once we've stolen them, don't let
            // anyone
            // steal from us
            final ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
            scrollIfNeeded(x);
            return true;
        }

        return false;
    }

    private void scrollIfNeeded(int x) {
        final int rawDeltaX = x - mMotionX;
        final int deltaX = rawDeltaX - mMotionCorrection;
        int incrementalDeltaX = mLastX != Integer.MIN_VALUE ? x - mLastX
                : deltaX;

        if (mTouchMode == TOUCH_MODE_SCROLL) {
            if (PROFILE_SCROLLING) {
                if (!mScrollProfilingStarted) {
                    Debug.startMethodTracing("AbsHorizontalListViewScroll");
                    mScrollProfilingStarted = true;
                }
            }

            if (x != mLastX) {
                // We may be here after stopping a fling and continuing to
                // scroll.
                // If so, we haven't disallowed intercepting touch events yet.
                // Make sure that we do so in case we're in a parent that can
                // intercept.
                if ((mHideApiProxy.mGroupFlags(this) & FLAG_DISALLOW_INTERCEPT) == 0
                        && Math.abs(rawDeltaX) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                final int motionIndex;
                if (mMotionPosition >= 0) {
                    motionIndex = mMotionPosition - mFirstPosition;
                } else {
                    // If we don't have a motion position that we can reliably
                    // track,
                    // pick something in the middle to make a best guess at
                    // things below.
                    motionIndex = getChildCount() / 2;
                }

                int motionViewPrevLeft = 0;
                View motionView = this.getChildAt(motionIndex);
                if (motionView != null) {
                    motionViewPrevLeft = motionView.getLeft();
                }

                // No need to do all this work if we're not going to move anyway
                boolean atEdge = false;
                if (incrementalDeltaX != 0) {
                    atEdge = trackMotionScroll(deltaX, incrementalDeltaX);
                }

                // Check to see if we have bumped into the scroll limit
                motionView = this.getChildAt(motionIndex);
                if (motionView != null) {
                    // Check if the top of the motion view is where it is
                    // supposed to be
                    final int motionViewRealLeft = motionView.getLeft();
                    if (atEdge) {
                        // Apply overscroll

                        int overscroll = -incrementalDeltaX
                                - (motionViewRealLeft - motionViewPrevLeft);
                        overScrollBy(overscroll, 0, getScrollX(), 0, 0, 0,
                                mOverscrollDistance, 0, true);
                        if (Math.abs(mOverscrollDistance) == Math
                                .abs(getScrollX())) {
                            // Don't allow overfling if we're at the edge.
                            if (mVelocityTracker != null) {
                                mVelocityTracker.clear();
                            }
                        }

                        final int overscrollMode = getOverScrollMode();
                        if (overscrollMode == OVER_SCROLL_ALWAYS
                                || (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && !contentFits())) {
                            mDirection = 0; // Reset when entering overscroll.
                            mTouchMode = TOUCH_MODE_OVERSCROLL;
                        }
                    }
                    mMotionX = x;
                }
                mLastX = x;
            }
        } else if (mTouchMode == TOUCH_MODE_OVERSCROLL) {
            if (x != mLastX) {
                final int oldScroll = getScrollX();
                final int newScroll = oldScroll - incrementalDeltaX;
                int newDirection = x > mLastX ? 1 : -1;

                if (mDirection == 0) {
                    mDirection = newDirection;
                }

                int overScrollDistance = -incrementalDeltaX;
                if ((newScroll < 0 && oldScroll >= 0)
                        || (newScroll > 0 && oldScroll <= 0)) {
                    overScrollDistance = -oldScroll;
                    incrementalDeltaX += overScrollDistance;
                } else {
                    incrementalDeltaX = 0;
                }

                if (overScrollDistance != 0) {
                    overScrollBy(overScrollDistance, 0, getScrollX(), 0, 0, 0,
                            mOverscrollDistance, 0, true);
                    final int overscrollMode = getOverScrollMode();
                    if (overscrollMode == OVER_SCROLL_ALWAYS
                            || (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && !contentFits())) {
                    }
                }

                if (incrementalDeltaX != 0) {
                    // Coming back to 'real' list scrolling
                    if (getScrollX() != 0) {
                        mHideApiProxy.mScrollX(this, 0);
                        mHideApiProxy.invalidateParentIfNeeded(this);
                    }

                    trackMotionScroll(incrementalDeltaX, incrementalDeltaX);

                    mTouchMode = TOUCH_MODE_SCROLL;

                    // We did not scroll the full amount. Treat this essentially
                    // like the
                    // start of a new touch scroll
                    final int motionPosition = findClosestMotionCol(x);

                    mMotionCorrection = 0;
                    View motionView = getChildAt(motionPosition
                            - mFirstPosition);
                    mMotionViewOriginalLeft = motionView != null ? motionView
                            .getLeft() : 0;
                    mMotionX = x;
                    mMotionPosition = motionPosition;
                }
                mLastX = x;
                mDirection = newDirection;
            }
        }
    }

    public void onTouchModeChanged(boolean isInTouchMode) {
        if (isInTouchMode) {
            // Get rid of the selection when we enter touch mode
            hideSelector();
            // Layout, but only if we already have done so previously.
            // (Otherwise may clobber a LAYOUT_SYNC layout that was requested to
            // restore
            // state.)
            if (getWidth() > 0 && getChildCount() > 0) {
                // We do not lose focus initiating a touch (since
                // AbsHorizontalListView is
                // focusable in
                // touch mode). Force an initial layout to get rid of the
                // selection.
                layoutChildren();
            }
            updateSelectorState();
        } else {
            int touchMode = mTouchMode;
            if (touchMode == TOUCH_MODE_OVERSCROLL
                    || touchMode == TOUCH_MODE_OVERFLING) {
                if (mFlingRunnable != null) {
                    mFlingRunnable.endFling();
                }
                if (mPositionScroller != null) {
                    mPositionScroller.stop();
                }

                if (getScrollX() != 0) {
                    mHideApiProxy.mScrollX(this, 0);
                    mHideApiProxy.invalidateParentCaches(this);
                    invalidate();
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return isClickable() || isLongClickable();
        }

        if (mPositionScroller != null) {
            mPositionScroller.stop();
        }

        if (!mIsAttached) {
            // Something isn't right.
            // Since we rely on being attached to get data set change
            // notifications,
            // don't risk doing anything where we might try to resync and find
            // things
            // in a bogus state.
            return false;
        }

        final int action = ev.getAction();

        View v;

        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            switch (mTouchMode) {
            case TOUCH_MODE_OVERFLING: {
                mFlingRunnable.endFling();
                if (mPositionScroller != null) {
                    mPositionScroller.stop();
                }
                mTouchMode = TOUCH_MODE_OVERSCROLL;
                mMotionX = mLastX = (int) ev.getX();
                mMotionY = (int) ev.getY();
                mMotionCorrection = 0;
                mActivePointerId = ev.getPointerId(0);
                mDirection = 0;
                break;
            }

            default: {
                mActivePointerId = ev.getPointerId(0);
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                int motionPosition = pointToPosition(x, y);
                if (!mDataChanged) {
                    if ((mTouchMode != TOUCH_MODE_FLING)
                            && (motionPosition >= 0)
                            && (getAdapter().isEnabled(motionPosition))) {
                        // User clicked on an actual view (and was not stopping
                        // a fling).
                        // It might be a click or a scroll. Assume it is a click
                        // until
                        // proven otherwise
                        mTouchMode = TOUCH_MODE_DOWN;
                        // FIXME Debounce
                        if (mPendingCheckForTap == null) {
                            mPendingCheckForTap = new CheckForTap();
                        }
                        postDelayed(mPendingCheckForTap,
                                ViewConfiguration.getTapTimeout());
                    } else {
                        if (mTouchMode == TOUCH_MODE_FLING) {
                            // Stopped a fling. It is a scroll.
                            createScrollingCache();
                            mTouchMode = TOUCH_MODE_SCROLL;
                            mMotionCorrection = 0;
                            motionPosition = findMotionCol(x);
                            mFlingRunnable.flywheelTouch();
                        }
                    }
                }

                if (motionPosition >= 0) {
                    // Remember where the motion event started
                    v = getChildAt(motionPosition - mFirstPosition);
                    mMotionViewOriginalLeft = v.getLeft();
                }
                mMotionX = x;
                mMotionY = y;
                mMotionPosition = motionPosition;
                mLastX = Integer.MIN_VALUE;
                break;
            }
            }

            if (mHideApiProxy.performButtonActionOnTouchDown(this, ev)) {
                if (mTouchMode == TOUCH_MODE_DOWN) {
                    removeCallbacks(mPendingCheckForTap);
                }
            }
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            int pointerIndex = ev.findPointerIndex(mActivePointerId);
            if (pointerIndex == -1) {
                pointerIndex = 0;
                mActivePointerId = ev.getPointerId(pointerIndex);
            }
            final int x = (int) ev.getX(pointerIndex);

            if (mDataChanged) {
                // Re-sync everything if data has been changed
                // since the scroll operation can query the adapter.
                layoutChildren();
            }

            switch (mTouchMode) {
            case TOUCH_MODE_DOWN:
            case TOUCH_MODE_TAP:
            case TOUCH_MODE_DONE_WAITING:
                // Check if we have moved far enough that it looks more like a
                // scroll than a tap
                startScrollIfNeeded(x);
                break;
            case TOUCH_MODE_SCROLL:
            case TOUCH_MODE_OVERSCROLL:
                scrollIfNeeded(x);
                break;
            }
            break;
        }

        case MotionEvent.ACTION_UP: {
            switch (mTouchMode) {
            case TOUCH_MODE_DOWN:
            case TOUCH_MODE_TAP:
            case TOUCH_MODE_DONE_WAITING:
                final int motionPosition = mMotionPosition;
                final View child = getChildAt(motionPosition - mFirstPosition);

                final float y = ev.getX();
                final boolean inList = y > mListPadding.top
                        && y < getHeight() - mListPadding.bottom;

                if (child != null && !child.hasFocusable() && inList) {
                    if (mTouchMode != TOUCH_MODE_DOWN) {
                        child.setPressed(false);
                    }

                    if (mPerformClick == null) {
                        mPerformClick = new PerformClick();
                    }

                    final AbsHorizontalListView.PerformClick performClick = mPerformClick;
                    performClick.mClickMotionPosition = motionPosition;
                    performClick.rememberWindowAttachCount();

                    mResurrectToPosition = motionPosition;

                    if (mTouchMode == TOUCH_MODE_DOWN
                            || mTouchMode == TOUCH_MODE_TAP) {
                        final Handler handler = getHandler();
                        if (handler != null) {
                            handler.removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ? mPendingCheckForTap
                                    : mPendingCheckForLongPress);
                        }
                        mLayoutMode = LAYOUT_NORMAL;
                        if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
                            mTouchMode = TOUCH_MODE_TAP;
                            setSelectedPositionInt(mMotionPosition);
                            layoutChildren();
                            child.setPressed(true);
                            positionSelector(mMotionPosition, child);
                            setPressed(true);
                            if (mSelector != null) {
                                Drawable d = mSelector.getCurrent();
                                if (d != null
                                        && d instanceof TransitionDrawable) {
                                    ((TransitionDrawable) d).resetTransition();
                                }
                            }
                            if (mTouchModeReset != null) {
                                removeCallbacks(mTouchModeReset);
                            }
                            mTouchModeReset = new Runnable() {
                                @Override
                                public void run() {
                                    mTouchMode = TOUCH_MODE_REST;
                                    child.setPressed(false);
                                    setPressed(false);
                                    if (!mDataChanged) {
                                        performClick.run();
                                    }
                                }
                            };
                            postDelayed(mTouchModeReset,
                                    ViewConfiguration.getPressedStateDuration());
                        } else {
                            mTouchMode = TOUCH_MODE_REST;
                            updateSelectorState();
                        }
                        return true;
                    } else if (!mDataChanged
                            && mAdapter.isEnabled(motionPosition)) {
                        performClick.run();
                    }
                }
                mTouchMode = TOUCH_MODE_REST;
                updateSelectorState();
                break;
            case TOUCH_MODE_SCROLL:
                final int childCount = getChildCount();
                if (childCount > 0) {
                    final int firstChildLeft = getChildAt(0).getLeft();
                    final int lastChildRight = getChildAt(childCount - 1)
                            .getRight();
                    final int contentLeft = mListPadding.left;
                    final int contentRight = getWidth() - mListPadding.right;
                    if (mFirstPosition == 0 && firstChildLeft >= contentLeft
                            && mFirstPosition + childCount < mItemCount
                            && lastChildRight <= getWidth() - contentRight) {
                        mTouchMode = TOUCH_MODE_REST;
                        reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    } else {
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000,
                                mMaximumVelocity);

                        final int initialVelocity = (int) (velocityTracker
                                .getXVelocity(mActivePointerId) * mVelocityScale);
                        // Fling if we have enough velocity and we aren't at a
                        // boundary.
                        // Since we can potentially overfling more than we can
                        // overscroll, don't
                        // allow the weird behavior where you can scroll to a
                        // boundary then
                        // fling further.
                        if (Math.abs(initialVelocity) > mMinimumVelocity
                                && !((mFirstPosition == 0 && firstChildLeft == contentLeft
                                        - mOverscrollDistance) || (mFirstPosition
                                        + childCount == mItemCount && lastChildRight == contentRight
                                        + mOverscrollDistance))) {
                            if (mFlingRunnable == null) {
                                mFlingRunnable = new FlingRunnable();
                            }
                            reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);

                            mFlingRunnable.start(-initialVelocity);
                        } else {
                            mTouchMode = TOUCH_MODE_REST;
                            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                            if (mFlingRunnable != null) {
                                mFlingRunnable.endFling();
                            }
                            if (mPositionScroller != null) {
                                mPositionScroller.stop();
                            }
                        }
                    }
                } else {
                    mTouchMode = TOUCH_MODE_REST;
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }
                break;

            case TOUCH_MODE_OVERSCROLL:
                if (mFlingRunnable == null) {
                    mFlingRunnable = new FlingRunnable();
                }
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final int initialVelocity = (int) velocityTracker
                        .getXVelocity(mActivePointerId);

                reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                if (Math.abs(initialVelocity) > mMinimumVelocity) {
                    mFlingRunnable.startOverfling(-initialVelocity);
                } else {
                    mFlingRunnable.startSpringback();
                }

                break;
            }

            setPressed(false);

            // Need to redraw since we probably aren't drawing the selector
            // anymore
            invalidate();

            final Handler handler = getHandler();
            if (handler != null) {
                handler.removeCallbacks(mPendingCheckForLongPress);
            }

            recycleVelocityTracker();

            mActivePointerId = INVALID_POINTER;

            if (PROFILE_SCROLLING) {
                if (mScrollProfilingStarted) {
                    Debug.stopMethodTracing();
                    mScrollProfilingStarted = false;
                }
            }
            break;
        }

        case MotionEvent.ACTION_CANCEL: {
            switch (mTouchMode) {
            case TOUCH_MODE_OVERSCROLL:
                if (mFlingRunnable == null) {
                    mFlingRunnable = new FlingRunnable();
                }
                mFlingRunnable.startSpringback();
                break;

            case TOUCH_MODE_OVERFLING:
                // Do nothing - let it play out.
                break;

            default:
                mTouchMode = TOUCH_MODE_REST;
                setPressed(false);
                View motionView = this.getChildAt(mMotionPosition
                        - mFirstPosition);
                if (motionView != null) {
                    motionView.setPressed(false);
                }
                clearScrollingCache();

                final Handler handler = getHandler();
                if (handler != null) {
                    handler.removeCallbacks(mPendingCheckForLongPress);
                }

                recycleVelocityTracker();
            }

            mActivePointerId = INVALID_POINTER;
            break;
        }

        case MotionEvent.ACTION_POINTER_UP: {
            onSecondaryPointerUp(ev);
            final int x = mMotionX;
            final int y = mMotionY;
            final int motionPosition = pointToPosition(x, y);
            if (motionPosition >= 0) {
                // Remember where the motion event started
                v = getChildAt(motionPosition - mFirstPosition);
                mMotionViewOriginalLeft = v.getLeft();
                mMotionPosition = motionPosition;
            }
            mLastX = x;
            break;
        }

        case MotionEvent.ACTION_POINTER_DOWN: {
            // New pointers take over dragging duties
            final int index = ev.getActionIndex();
            final int id = ev.getPointerId(index);
            final int x = (int) ev.getX(index);
            final int y = (int) ev.getY(index);
            mMotionCorrection = 0;
            mActivePointerId = id;
            mMotionX = x;
            mMotionY = y;
            final int motionPosition = pointToPosition(x, y);
            if (motionPosition >= 0) {
                // Remember where the motion event started
                v = getChildAt(motionPosition - mFirstPosition);
                mMotionViewOriginalLeft = v.getLeft();
                mMotionPosition = motionPosition;
            }
            mLastX = x;
            break;
        }
        }

        return true;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
            boolean clampedY) {
        if (getScrollX() != scrollX) {
            onScrollChanged(scrollX, getScrollY(), getScrollX(), getScrollY());
            mHideApiProxy.mScrollX(this, scrollX);
            mHideApiProxy.invalidateParentIfNeeded(this);

            awakenScrollBars();
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_SCROLL: {
                if (mTouchMode == TOUCH_MODE_REST) {
                    final float hscroll = event
                            .getAxisValue(MotionEvent.AXIS_HSCROLL);
                    if (hscroll != 0) {
                        final int delta = (int) (hscroll * mHideApiProxy
                                .getHorizontalScrollFactor(this));
                        if (!trackMotionScroll(delta, delta)) {
                            return true;
                        }
                    }
                }
            }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        View v;

        if (mPositionScroller != null) {
            mPositionScroller.stop();
        }

        if (!mIsAttached) {
            // Something isn't right.
            // Since we rely on being attached to get data set change
            // notifications,
            // don't risk doing anything where we might try to resync and find
            // things
            // in a bogus state.
            return false;
        }

        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            int touchMode = mTouchMode;
            if (touchMode == TOUCH_MODE_OVERFLING
                    || touchMode == TOUCH_MODE_OVERSCROLL) {
                mMotionCorrection = 0;
                return true;
            }

            final int x = (int) ev.getX();
            final int y = (int) ev.getY();
            mActivePointerId = ev.getPointerId(0);

            int motionPosition = findMotionCol(x);
            if (touchMode != TOUCH_MODE_FLING && motionPosition >= 0) {
                // User clicked on an actual view (and was not stopping a
                // fling).
                // Remember where the motion event started
                v = getChildAt(motionPosition - mFirstPosition);
                mMotionViewOriginalLeft = v.getLeft();
                mMotionX = x;
                mMotionY = y;
                mMotionPosition = motionPosition;
                mTouchMode = TOUCH_MODE_DOWN;
                clearScrollingCache();
            }
            mLastX = Integer.MIN_VALUE;
            initOrResetVelocityTracker();
            mVelocityTracker.addMovement(ev);
            if (touchMode == TOUCH_MODE_FLING) {
                return true;
            }
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            switch (mTouchMode) {
            case TOUCH_MODE_DOWN:
                int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex == -1) {
                    pointerIndex = 0;
                    mActivePointerId = ev.getPointerId(pointerIndex);
                }
                final int x = (int) ev.getX(pointerIndex);
                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(ev);
                if (startScrollIfNeeded(x)) {
                    return true;
                }
                break;
            }
            break;
        }

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP: {
            mTouchMode = TOUCH_MODE_REST;
            mActivePointerId = INVALID_POINTER;
            recycleVelocityTracker();
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            break;
        }

        case MotionEvent.ACTION_POINTER_UP: {
            onSecondaryPointerUp(ev);
            break;
        }
        }

        return false;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mMotionX = (int) ev.getX(newPointerIndex);
            mMotionY = (int) ev.getY(newPointerIndex);
            mMotionCorrection = 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTouchables(ArrayList<View> views) {
        final int count = getChildCount();
        final int firstPosition = mFirstPosition;
        final ListAdapter adapter = mAdapter;

        if (adapter == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (adapter.isEnabled(firstPosition + i)) {
                views.add(child);
            }
            child.addTouchables(views);
        }
    }

    /**
     * Fires an "on scroll state changed" event to the registered
     * {@link android.widget.AbsHorizontalListView.OnScrollListener}, if any.
     * The state change is fired only if the specified state is different from
     * the previously known state.
     * 
     * @param newState
     *            The new scroll state.
     */
    void reportScrollStateChange(int newState) {
        if (newState != mLastScrollState) {
            if (mOnScrollListener != null) {
                mLastScrollState = newState;
                mOnScrollListener.onScrollStateChanged(this, newState);
            }
        }
    }

    /**
     * Responsible for fling behavior. Use {@link #start(int)} to initiate a
     * fling. Each frame of the fling is handled in {@link #run()}. A
     * FlingRunnable will keep re-posting itself until the fling is done.
     * 
     */
    private class FlingRunnable implements Runnable {
        /**
         * Tracks the decay of a fling scroll
         */
        private final OverScroller mScroller;

        /**
         * X value reported by mScroller on the previous fling
         */
        private int mLastFlingX;

        private final Runnable mCheckFlywheel = new Runnable() {
            public void run() {
                final int activeId = mActivePointerId;
                final VelocityTracker vt = mVelocityTracker;
                final OverScroller scroller = mScroller;
                if (vt == null || activeId == INVALID_POINTER) {
                    return;
                }

                vt.computeCurrentVelocity(1000, mMaximumVelocity);
                final float xvel = -vt.getXVelocity(activeId);

                if (Math.abs(xvel) >= mMinimumVelocity
                        && mHideApiProxy.isScrollingInDirection(scroller, xvel,
                                0)) {
                    // Keep the fling alive a little longer
                    postDelayed(this, FLYWHEEL_TIMEOUT);
                } else {
                    endFling();
                    mTouchMode = TOUCH_MODE_SCROLL;
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                }
            }
        };

        private static final int FLYWHEEL_TIMEOUT = 40; // milliseconds

        FlingRunnable() {
            mScroller = new OverScroller(getContext());
        }

        void start(int initialVelocity) {
            int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            mLastFlingX = initialX;
            mScroller.fling(initialX, 0, initialVelocity, 0, Integer.MAX_VALUE,
                    0, Integer.MAX_VALUE, 0);
            mTouchMode = TOUCH_MODE_FLING;
            post(this);

            if (PROFILE_FLINGING) {
                if (!mFlingProfilingStarted) {
                    Debug.startMethodTracing("AbsListViewFling");
                    mFlingProfilingStarted = true;
                }
            }

            // if (mFlingStrictSpan == null) {
            // mFlingStrictSpan = StrictMode
            // .enterCriticalSpan("AbsListView-fling");
            // }
        }

        void startSpringback() {
            if (mScroller.springBack(getScrollX(), 0, 0, 0, 0, 0)) {
                mTouchMode = TOUCH_MODE_OVERFLING;
                invalidate();
                post(this);
            } else {
                mTouchMode = TOUCH_MODE_REST;
                reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            }
        }

        void startOverfling(int initialVelocity) {
            mScroller.fling(getScrollX(), 0, initialVelocity, 0, 0,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, 0, getWidth(), 0);
            mTouchMode = TOUCH_MODE_OVERFLING;
            invalidate();
            post(this);
        }

        void edgeReached(int delta) {
            mScroller.notifyHorizontalEdgeReached(getScrollX(), 0,
                    mOverflingDistance);
            final int overscrollMode = getOverScrollMode();
            if (overscrollMode == OVER_SCROLL_ALWAYS
                    || (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && !contentFits())) {
                mTouchMode = TOUCH_MODE_OVERFLING;
                // final int vel = (int) mScroller.getCurrVelocity();
                // if (delta > 0) {
                // mEdgeGlowTop.onAbsorb(vel);
                // } else {
                // mEdgeGlowBottom.onAbsorb(vel);
                // }
            } else {
                mTouchMode = TOUCH_MODE_REST;
                if (mPositionScroller != null) {
                    mPositionScroller.stop();
                }
            }
            invalidate();
            post(this);
        }

        void startScroll(int distance, int duration) {
            int initialX = distance < 0 ? Integer.MAX_VALUE : 0;
            mLastFlingX = initialX;
            mScroller.startScroll(initialX, 0, distance, 0, duration);
            mTouchMode = TOUCH_MODE_FLING;
            post(this);
        }

        void endFling() {
            mTouchMode = TOUCH_MODE_REST;

            removeCallbacks(this);
            removeCallbacks(mCheckFlywheel);

            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            clearScrollingCache();
            mScroller.abortAnimation();

            // if (mFlingStrictSpan != null) {
            // mFlingStrictSpan.finish();
            // mFlingStrictSpan = null;
            // }
        }

        void flywheelTouch() {
            postDelayed(mCheckFlywheel, FLYWHEEL_TIMEOUT);
        }

        public void run() {
            switch (mTouchMode) {
            default:
                endFling();
                return;

            case TOUCH_MODE_SCROLL:
                if (mScroller.isFinished()) {
                    return;
                }
                // Fall through
            case TOUCH_MODE_FLING: {
                if (mDataChanged) {
                    layoutChildren();
                }

                if (mItemCount == 0 || getChildCount() == 0) {
                    endFling();
                    return;
                }

                final OverScroller scroller = mScroller;
                boolean more = scroller.computeScrollOffset();
                final int x = scroller.getCurrX();

                // Flip sign to convert finger direction to list items direction
                // (e.g. finger moving down means list is moving towards the
                // top)
                int delta = mLastFlingX - x;

                // Pretend that each frame of a fling scroll is a touch scroll
                if (delta > 0) {
                    // List is moving towards the top. Use first view as
                    // mMotionPosition
                    mMotionPosition = mFirstPosition;
                    final View firstView = getChildAt(0);
                    mMotionViewOriginalLeft = firstView.getLeft();

                    // Don't fling more than 1 screen
                    delta = Math.min(getWidth() - getPaddingRight()
                            - getPaddingLeft() - 1, delta);
                } else {
                    // List is moving towards the bottom. Use last view as
                    // mMotionPosition
                    int offsetToLast = getChildCount() - 1;
                    mMotionPosition = mFirstPosition + offsetToLast;

                    final View lastView = getChildAt(offsetToLast);
                    mMotionViewOriginalLeft = lastView.getLeft();

                    // Don't fling more than 1 screen
                    delta = Math.max(-(getWidth() - getPaddingRight()
                            - getPaddingLeft() - 1), delta);
                }

                // Check to see if we have bumped into the scroll limit
                View motionView = getChildAt(mMotionPosition - mFirstPosition);
                int oldLeft = 0;
                if (motionView != null) {
                    oldLeft = motionView.getLeft();
                }

                // Don't stop just because delta is zero (it could have been
                // rounded)
                final boolean atEnd = trackMotionScroll(delta, delta)
                        && (delta != 0);
                if (atEnd) {
                    if (motionView != null) {
                        // Tweak the scroll for how far we overshot
                        int overshoot = -(delta - (motionView.getLeft() - oldLeft));
                        overScrollBy(overshoot, 0, getScrollX(), 0, 0, 0,
                                mOverflingDistance, 0, false);
                    }
                    if (more) {
                        edgeReached(delta);
                    }
                    break;
                }

                if (more && !atEnd) {
                    invalidate();
                    mLastFlingX = x;
                    post(this);
                } else {
                    endFling();

                    if (PROFILE_FLINGING) {
                        if (mFlingProfilingStarted) {
                            Debug.stopMethodTracing();
                            mFlingProfilingStarted = false;
                        }

                        // if (mFlingStrictSpan != null) {
                        // mFlingStrictSpan.finish();
                        // mFlingStrictSpan = null;
                        // }
                    }
                }
                break;
            }

            case TOUCH_MODE_OVERFLING: {
                final OverScroller scroller = mScroller;
                if (scroller.computeScrollOffset()) {
                    final int scrollX = getScrollX();
                    final int currX = scroller.getCurrX();
                    final int deltaX = currX - scrollX;
                    if (overScrollBy(deltaX, 0, scrollX, 0, 0, 0,
                            mOverflingDistance, 0, false)) {
                        final boolean crossRight = scrollX <= 0 && currX > 0;
                        final boolean crossLeft = scrollX >= 0 && currX < 0;
                        if (crossRight || crossLeft) {
                            int velocity = (int) scroller.getCurrVelocity();
                            if (crossLeft)
                                velocity = -velocity;

                            // Don't flywheel from this; we're just continuing
                            // things.
                            scroller.abortAnimation();
                            start(velocity);
                        } else {
                            startSpringback();
                        }
                    } else {
                        invalidate();
                        post(this);
                    }
                } else {
                    endFling();
                }
                break;
            }
            }
        }
    }

    class PositionScroller implements Runnable {
        private static final int SCROLL_DURATION = 200;

        private static final int MOVE_RIGHT_POS = 1;
        private static final int MOVE_LEFT_POS = 2;
        private static final int MOVE_RIGHT_BOUND = 3;
        private static final int MOVE_LEFT_BOUND = 4;
        private static final int MOVE_OFFSET = 5;

        private int mMode;
        private int mTargetPos;
        private int mBoundPos;
        private int mLastSeenPos;
        private int mScrollDuration;
        private final int mExtraScroll;

        private int mOffsetFromLeft;

        PositionScroller() {
            mExtraScroll = ViewConfiguration.get(getContext())
                    .getScaledFadingEdgeLength();
        }

        void start(final int position) {
            stop();

            if (mDataChanged) {
                // Wait until we're back in a stable state to try this.
                mPositionScrollAfterLayout = new Runnable() {
                    @Override
                    public void run() {
                        start(position);
                    }
                };
                return;
            }

            final int childCount = getChildCount();
            if (childCount == 0) {
                // Can't scroll without children.
                return;
            }

            final int firstPos = mFirstPosition;
            final int lastPos = firstPos + childCount - 1;

            int viewTravelCount;
            int clampedPosition = Math.max(0,
                    Math.min(getCount() - 1, position));
            if (clampedPosition < firstPos) {
                viewTravelCount = firstPos - clampedPosition + 1;
                mMode = MOVE_LEFT_POS;
            } else if (clampedPosition > lastPos) {
                viewTravelCount = clampedPosition - lastPos + 1;
                mMode = MOVE_RIGHT_POS;
            } else {
                scrollToVisible(clampedPosition, INVALID_POSITION,
                        SCROLL_DURATION);
                return;
            }

            if (viewTravelCount > 0) {
                mScrollDuration = SCROLL_DURATION / viewTravelCount;
            } else {
                mScrollDuration = SCROLL_DURATION;
            }
            mTargetPos = clampedPosition;
            mBoundPos = INVALID_POSITION;
            mLastSeenPos = INVALID_POSITION;

            postOnAnimation(this);
        }

        void start(final int position, final int boundPosition) {
            stop();

            if (boundPosition == INVALID_POSITION) {
                start(position);
                return;
            }

            if (mDataChanged) {
                // Wait until we're back in a stable state to try this.
                mPositionScrollAfterLayout = new Runnable() {
                    @Override
                    public void run() {
                        start(position, boundPosition);
                    }
                };
                return;
            }

            final int childCount = getChildCount();
            if (childCount == 0) {
                // Can't scroll without children.
                return;
            }

            final int firstPos = mFirstPosition;
            final int lastPos = firstPos + childCount - 1;

            int viewTravelCount;
            int clampedPosition = Math.max(0,
                    Math.min(getCount() - 1, position));
            if (clampedPosition < firstPos) {
                final int boundPosFromLast = lastPos - boundPosition;
                if (boundPosFromLast < 1) {
                    // Moving would shift our bound position off the screen.
                    // Abort.
                    return;
                }

                final int posTravel = firstPos - clampedPosition + 1;
                final int boundTravel = boundPosFromLast - 1;
                if (boundTravel < posTravel) {
                    viewTravelCount = boundTravel;
                    mMode = MOVE_LEFT_BOUND;
                } else {
                    viewTravelCount = posTravel;
                    mMode = MOVE_LEFT_POS;
                }
            } else if (clampedPosition > lastPos) {
                final int boundPosFromFirst = boundPosition - firstPos;
                if (boundPosFromFirst < 1) {
                    // Moving would shift our bound position off the screen.
                    // Abort.
                    return;
                }

                final int posTravel = clampedPosition - lastPos + 1;
                final int boundTravel = boundPosFromFirst - 1;
                if (boundTravel < posTravel) {
                    viewTravelCount = boundTravel;
                    mMode = MOVE_RIGHT_BOUND;
                } else {
                    viewTravelCount = posTravel;
                    mMode = MOVE_RIGHT_POS;
                }
            } else {
                scrollToVisible(clampedPosition, boundPosition, SCROLL_DURATION);
                return;
            }

            if (viewTravelCount > 0) {
                mScrollDuration = SCROLL_DURATION / viewTravelCount;
            } else {
                mScrollDuration = SCROLL_DURATION;
            }
            mTargetPos = clampedPosition;
            mBoundPos = boundPosition;
            mLastSeenPos = INVALID_POSITION;

            postOnAnimation(this);
        }

        void startWithOffset(int position, int offset) {
            startWithOffset(position, offset, SCROLL_DURATION);
        }

        void startWithOffset(final int position, int offset, final int duration) {
            stop();

            if (mDataChanged) {
                // Wait until we're back in a stable state to try this.
                final int postOffset = offset;
                mPositionScrollAfterLayout = new Runnable() {
                    @Override
                    public void run() {
                        startWithOffset(position, postOffset, duration);
                    }
                };
                return;
            }

            final int childCount = getChildCount();
            if (childCount == 0) {
                // Can't scroll without children.
                return;
            }

            offset += getPaddingLeft();

            mTargetPos = Math.max(0, Math.min(getCount() - 1, position));
            mOffsetFromLeft = offset;
            mBoundPos = INVALID_POSITION;
            mLastSeenPos = INVALID_POSITION;
            mMode = MOVE_OFFSET;

            final int firstPos = mFirstPosition;
            final int lastPos = firstPos + childCount - 1;

            int viewTravelCount;
            if (mTargetPos < firstPos) {
                viewTravelCount = firstPos - mTargetPos;
            } else if (mTargetPos > lastPos) {
                viewTravelCount = mTargetPos - lastPos;
            } else {
                // On-screen, just scroll.
                final int targetLeft = getChildAt(mTargetPos - firstPos)
                        .getLeft();
                smoothScrollBy(targetLeft - offset, duration, true);
                return;
            }

            // Estimate how many screens we should travel
            final float screenTravelCount = (float) viewTravelCount
                    / childCount;
            mScrollDuration = screenTravelCount < 1 ? duration
                    : (int) (duration / screenTravelCount);
            mLastSeenPos = INVALID_POSITION;

            postOnAnimation(this);
        }

        /**
         * Scroll such that targetPos is in the visible padded region without
         * scrolling boundPos out of view. Assumes targetPos is onscreen.
         */
        void scrollToVisible(int targetPos, int boundPos, int duration) {
            final int firstPos = mFirstPosition;
            final int childCount = getChildCount();
            final int lastPos = firstPos + childCount - 1;
            final int paddedTop = mListPadding.top;
            final int paddedBottom = getHeight() - mListPadding.bottom;

            if (targetPos < firstPos || targetPos > lastPos) {
                Log.w(TAG, "scrollToVisible called with targetPos " + targetPos
                        + " not visible [" + firstPos + ", " + lastPos + "]");
            }
            if (boundPos < firstPos || boundPos > lastPos) {
                // boundPos doesn't matter, it's already offscreen.
                boundPos = INVALID_POSITION;
            }

            final View targetChild = getChildAt(targetPos - firstPos);
            final int targetTop = targetChild.getTop();
            final int targetBottom = targetChild.getBottom();
            int scrollBy = 0;

            if (targetBottom > paddedBottom) {
                scrollBy = targetBottom - paddedBottom;
            }
            if (targetTop < paddedTop) {
                scrollBy = targetTop - paddedTop;
            }

            if (scrollBy == 0) {
                return;
            }

            if (boundPos >= 0) {
                final View boundChild = getChildAt(boundPos - firstPos);
                final int boundTop = boundChild.getTop();
                final int boundBottom = boundChild.getBottom();
                final int absScroll = Math.abs(scrollBy);

                if (scrollBy < 0 && boundBottom + absScroll > paddedBottom) {
                    // Don't scroll the bound view off the bottom of the screen.
                    scrollBy = Math.max(0, boundBottom - paddedBottom);
                } else if (scrollBy > 0 && boundTop - absScroll < paddedTop) {
                    // Don't scroll the bound view off the top of the screen.
                    scrollBy = Math.min(0, boundTop - paddedTop);
                }
            }

            smoothScrollBy(scrollBy, duration);
        }

        void stop() {
            removeCallbacks(this);
        }

        public void run() {
            final int listWidth = getWidth();
            final int firstPos = mFirstPosition;

            switch (mMode) {
            case MOVE_RIGHT_POS: {
                final int lastViewIndex = getChildCount() - 1;
                final int lastPos = firstPos + lastViewIndex;

                if (lastViewIndex < 0) {
                    return;
                }

                if (lastPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    postOnAnimation(this);
                    return;
                }

                final View lastView = getChildAt(lastViewIndex);
                final int lastViewWidth = lastView.getWidth();
                final int lastViewLeft = lastView.getLeft();
                final int lastViewPixelsShowing = listWidth - lastViewLeft;
                final int extraScroll = lastPos < mItemCount - 1 ? Math.max(
                        mListPadding.right, mExtraScroll) : mListPadding.right;

                final int scrollBy = lastViewWidth - lastViewPixelsShowing
                        + extraScroll;
                smoothScrollBy(scrollBy, mScrollDuration, true);

                mLastSeenPos = lastPos;
                if (lastPos < mTargetPos) {
                    postOnAnimation(this);
                }
                break;
            }

            case MOVE_RIGHT_BOUND: {
                final int nextViewIndex = 1;
                final int childCount = getChildCount();

                if (firstPos == mBoundPos || childCount <= nextViewIndex
                        || firstPos + childCount >= mItemCount) {
                    return;
                }
                final int nextPos = firstPos + nextViewIndex;

                if (nextPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    postOnAnimation(this);
                    return;
                }

                final View nextView = getChildAt(nextViewIndex);
                final int nextViewWidth = nextView.getWidth();
                final int nextViewLeft = nextView.getLeft();
                final int extraScroll = Math.max(mListPadding.right,
                        mExtraScroll);
                if (nextPos < mBoundPos) {
                    smoothScrollBy(
                            Math.max(0, nextViewWidth + nextViewLeft
                                    - extraScroll), mScrollDuration, true);

                    mLastSeenPos = nextPos;

                    postOnAnimation(this);
                } else {
                    if (nextViewLeft > extraScroll) {
                        smoothScrollBy(nextViewLeft - extraScroll,
                                mScrollDuration, true);
                    }
                }
                break;
            }

            case MOVE_LEFT_POS: {
                if (firstPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    postOnAnimation(this);
                    return;
                }

                final View firstView = getChildAt(0);
                if (firstView == null) {
                    return;
                }
                final int firstViewLeft = firstView.getLeft();
                final int extraScroll = firstPos > 0 ? Math.max(mExtraScroll,
                        mListPadding.left) : mListPadding.left;

                smoothScrollBy(firstViewLeft - extraScroll, mScrollDuration,
                        true);

                mLastSeenPos = firstPos;

                if (firstPos > mTargetPos) {
                    postOnAnimation(this);
                }
                break;
            }

            case MOVE_LEFT_BOUND: {
                final int lastViewIndex = getChildCount() - 2;
                if (lastViewIndex < 0) {
                    return;
                }
                final int lastPos = firstPos + lastViewIndex;

                if (lastPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    postOnAnimation(this);
                    return;
                }

                final View lastView = getChildAt(lastViewIndex);
                final int lastViewWidth = lastView.getWidth();
                final int lastViewLeft = lastView.getLeft();
                final int lastViewPixelsShowing = listWidth - lastViewLeft;
                final int extraScroll = Math.max(mListPadding.left,
                        mExtraScroll);
                mLastSeenPos = lastPos;
                if (lastPos > mBoundPos) {
                    smoothScrollBy(-(lastViewPixelsShowing - extraScroll),
                            mScrollDuration, true);
                    postOnAnimation(this);
                } else {
                    final int right = listWidth - extraScroll;
                    final int lastViewRight = lastViewLeft + lastViewWidth;
                    if (right > lastViewRight) {
                        smoothScrollBy(-(right - lastViewRight),
                                mScrollDuration, true);
                    }
                }
                break;
            }

            case MOVE_OFFSET: {
                if (mLastSeenPos == firstPos) {
                    // No new views, let things keep going.
                    postOnAnimation(this);
                    return;
                }

                mLastSeenPos = firstPos;

                final int childCount = getChildCount();
                final int position = mTargetPos;
                final int lastPos = firstPos + childCount - 1;

                int viewTravelCount = 0;
                if (position < firstPos) {
                    viewTravelCount = firstPos - position + 1;
                } else if (position > lastPos) {
                    viewTravelCount = position - lastPos;
                }

                // Estimate how many screens we should travel
                final float screenTravelCount = (float) viewTravelCount
                        / childCount;

                final float modifier = Math.min(Math.abs(screenTravelCount),
                        1.f);
                if (position < firstPos) {
                    final int distance = (int) (-getWidth() * modifier);
                    final int duration = (int) (mScrollDuration * modifier);
                    smoothScrollBy(distance, duration, true);
                    postOnAnimation(this);
                } else if (position > lastPos) {
                    final int distance = (int) (getWidth() * modifier);
                    final int duration = (int) (mScrollDuration * modifier);
                    smoothScrollBy(distance, duration, true);
                    postOnAnimation(this);
                } else {
                    // On-screen, just scroll.
                    final int targetLeft = getChildAt(position - firstPos)
                            .getLeft();
                    final int distance = targetLeft - mOffsetFromLeft;
                    final int duration = (int) (mScrollDuration * ((float) Math
                            .abs(distance) / getWidth()));
                    smoothScrollBy(distance, duration, true);
                }
                break;
            }

            default:
                break;
            }
        }
    }

    /**
     * The amount of friction applied to flings. The default value is
     * {@link ViewConfiguration#getScrollFriction}.
     * 
     * @return A scalar dimensionless value representing the coefficient of
     *         friction.
     */
    public void setFriction(float friction) {
        if (mFlingRunnable == null) {
            mFlingRunnable = new FlingRunnable();
        }
        mFlingRunnable.mScroller.setFriction(friction);
    }

    /**
     * Sets a scale factor for the fling velocity. The initial scale factor is
     * 1.0.
     * 
     * @param scale
     *            The scale factor to multiply the velocity by.
     */
    public void setVelocityScale(float scale) {
        mVelocityScale = scale;
    }

    /**
     * Smoothly scroll to the specified adapter position. The view will scroll
     * such that the indicated position is displayed.
     * 
     * @param position
     *            Scroll to this adapter position.
     */
    public void smoothScrollToPosition(int position) {
        if (mPositionScroller == null) {
            mPositionScroller = new PositionScroller();
        }
        mPositionScroller.start(position);
    }

    /**
     * Smoothly scroll to the specified adapter position. The view will scroll
     * such that the indicated position is displayed <code>offset</code> pixels
     * from the top edge of the view. If this is impossible, (e.g. the offset
     * would scroll the first or last item beyond the boundaries of the list) it
     * will get as close as possible. The scroll will take <code>duration</code>
     * milliseconds to complete.
     * 
     * @param position
     *            Position to scroll to
     * @param offset
     *            Desired distance in pixels of <code>position</code> from the
     *            top of the view when scrolling is finished
     * @param duration
     *            Number of milliseconds to use for the scroll
     */
    public void smoothScrollToPositionFromTop(int position, int offset,
            int duration) {
        if (mPositionScroller == null) {
            mPositionScroller = new PositionScroller();
        }
        mPositionScroller.startWithOffset(position, offset, duration);
    }

    /**
     * Smoothly scroll to the specified adapter position. The view will scroll
     * such that the indicated position is displayed <code>offset</code> pixels
     * from the top edge of the view. If this is impossible, (e.g. the offset
     * would scroll the first or last item beyond the boundaries of the list) it
     * will get as close as possible.
     * 
     * @param position
     *            Position to scroll to
     * @param offset
     *            Desired distance in pixels of <code>position</code> from the
     *            top of the view when scrolling is finished
     */
    public void smoothScrollToPositionFromTop(int position, int offset) {
        if (mPositionScroller == null) {
            mPositionScroller = new PositionScroller();
        }
        mPositionScroller.startWithOffset(position, offset);
    }

    /**
     * Smoothly scroll to the specified adapter position. The view will scroll
     * such that the indicated position is displayed, but it will stop early if
     * scrolling further would scroll boundPosition out of view.
     * 
     * @param position
     *            Scroll to this adapter position.
     * @param boundPosition
     *            Do not scroll if it would move this adapter position out of
     *            view.
     */
    public void smoothScrollToPosition(int position, int boundPosition) {
        if (mPositionScroller == null) {
            mPositionScroller = new PositionScroller();
        }
        mPositionScroller.start(position, boundPosition);
    }

    /**
     * Smoothly scroll by distance pixels over duration milliseconds.
     * 
     * @param distance
     *            Distance to scroll in pixels.
     * @param duration
     *            Duration of the scroll animation in milliseconds.
     */
    public void smoothScrollBy(int distance, int duration) {
        smoothScrollBy(distance, duration, false);
    }

    void smoothScrollBy(int distance, int duration, boolean linear) {
        if (mFlingRunnable == null) {
            mFlingRunnable = new FlingRunnable();
        }

        // No sense starting to scroll if we're not going anywhere
        final int firstPos = mFirstPosition;
        final int childCount = getChildCount();
        final int lastPos = firstPos + childCount;
        final int leftLimit = getPaddingLeft();
        final int rightLimit = getWidth() - getPaddingRight();

        if (distance == 0
                || mItemCount == 0
                || childCount == 0
                || (firstPos == 0 && getChildAt(0).getLeft() == leftLimit && distance < 0)
                || (lastPos == mItemCount - 1
                        && getChildAt(childCount - 1).getRight() == rightLimit && distance > 0)) {
            mFlingRunnable.endFling();
            if (mPositionScroller != null) {
                mPositionScroller.stop();
            }
        } else {
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
            mFlingRunnable.startScroll(distance, duration);
        }
    }

    /**
     * Allows RemoteViews to scroll relatively to a position.
     */
    void smoothScrollByOffset(int position) {
        int index = -1;
        if (position < 0) {
            index = getFirstVisiblePosition();
        } else if (position > 0) {
            index = getLastVisiblePosition();
        }

        if (index > -1) {
            View child = getChildAt(index - getFirstVisiblePosition());
            if (child != null) {
                Rect visibleRect = new Rect();
                if (child.getGlobalVisibleRect(visibleRect)) {
                    // the child is partially visible
                    int childRectArea = child.getWidth() * child.getHeight();
                    int visibleRectArea = visibleRect.width()
                            * visibleRect.height();
                    float visibleArea = (visibleRectArea / (float) childRectArea);
                    final float visibleThreshold = 0.75f;
                    if ((position < 0) && (visibleArea < visibleThreshold)) {
                        // the top index is not perceivably visible so offset
                        // to account for showing that top index as well
                        ++index;
                    } else if ((position > 0)
                            && (visibleArea < visibleThreshold)) {
                        // the bottom index is not perceivably visible so offset
                        // to account for showing that bottom index as well
                        --index;
                    }
                }
                smoothScrollToPosition(Math.max(0,
                        Math.min(getCount(), index + position)));
            }
        }
    }

    private void createScrollingCache() {
        if (mScrollingCacheEnabled && !mCachingStarted
                && !isHardwareAccelerated()) {
            setChildrenDrawnWithCacheEnabled(true);
            setChildrenDrawingCacheEnabled(true);
            mCachingStarted = mCachingActive = true;
        }
    }

    private void clearScrollingCache() {
        if (!isHardwareAccelerated()) {
            if (mClearScrollingCache == null) {
                mClearScrollingCache = new Runnable() {
                    public void run() {
                        if (mCachingStarted) {
                            mCachingStarted = mCachingActive = false;
                            setChildrenDrawnWithCacheEnabled(false);
                            if ((getPersistentDrawingCache() & PERSISTENT_SCROLLING_CACHE) == 0) {
                                setChildrenDrawingCacheEnabled(false);
                            }
                            if (!isAlwaysDrawnWithCacheEnabled()) {
                                invalidate();
                            }
                        }
                    }
                };
            }
            post(mClearScrollingCache);
        }
    }

    /**
     * Track a motion scroll
     * 
     * @param deltaX
     *            Amount to offset mMotionView. This is the accumulated delta
     *            since the motion began. Positive numbers mean the user's
     *            finger is moving down the screen.
     * @param incrementalDeltaX
     *            Change in deltaX from the previous event.
     * @return true if we're already at the beginning/end of the list and have
     *         nothing to do.
     */
    boolean trackMotionScroll(int deltaX, int incrementalDeltaX) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return true;
        }

        final int firstLeft = getChildAt(0).getLeft();
        final int lastRight = getChildAt(childCount - 1).getRight();

        final Rect listPadding = mListPadding;

        // "effective padding" In this case is the amount of padding that
        // affects
        // how much space should not be filled by items. If we don't clip to
        // padding
        // there is no effective padding.
        int effectivePaddingLeft = 0;
        int effectivePaddingRight = 0;
        if ((mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            effectivePaddingLeft = listPadding.left;
            effectivePaddingRight = listPadding.right;
        }

        // FIXME account for grid vertical spacing too?
        final int spacePrev = effectivePaddingLeft - firstLeft;
        final int end = getWidth() - effectivePaddingRight;
        final int spaceNext = lastRight - end;

        final int width = getWidth() - getPaddingRight() - getPaddingLeft();
        if (deltaX < 0) {
            deltaX = Math.max(-(width - 1), deltaX);
        } else {
            deltaX = Math.min(width - 1, deltaX);
        }

        if (incrementalDeltaX < 0) {
            incrementalDeltaX = Math.max(-(width - 1), incrementalDeltaX);
        } else {
            incrementalDeltaX = Math.min(width - 1, incrementalDeltaX);
        }

        final int firstPosition = mFirstPosition;

        // Update our guesses for where the first and last views are
        if (firstPosition == 0) {
            mFirstPositionDistanceGuess = firstLeft - listPadding.left;
        } else {
            mFirstPositionDistanceGuess += incrementalDeltaX;
        }
        if (firstPosition + childCount == mItemCount) {
            mLastPositionDistanceGuess = lastRight + listPadding.right;
        } else {
            mLastPositionDistanceGuess += incrementalDeltaX;
        }

        final boolean cannotScrollRight = (firstPosition == 0
                && firstLeft >= listPadding.left && incrementalDeltaX >= 0);
        final boolean cannotScrollLeft = (firstPosition + childCount == mItemCount
                && lastRight <= getWidth() - listPadding.right && incrementalDeltaX <= 0);

        if (cannotScrollRight || cannotScrollLeft) {
            return incrementalDeltaX != 0;
        }

        final boolean rightTo = incrementalDeltaX < 0;

        final boolean inTouchMode = isInTouchMode();
        if (inTouchMode) {
            hideSelector();
        }

        final int itemCount = mItemCount;

        int start = 0;
        int count = 0;

        if (rightTo) {
            int left = -incrementalDeltaX;
            if ((mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
                left += listPadding.left;
            }
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getRight() >= left) {
                    break;
                } else {
                    count++;
                    int position = firstPosition + i;
                    if (position >= 0 && position < itemCount) {
                        mRecycler.addScrapView(child, position);
                    }
                }
            }
        } else {
            int right = getWidth() - incrementalDeltaX;
            if ((mHideApiProxy.mGroupFlags(this) & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
                right -= listPadding.right;
            }
            for (int i = childCount - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                if (child.getLeft() <= right) {
                    break;
                } else {
                    start = i;
                    count++;
                    int position = firstPosition + i;
                    if (position >= 0 && position < itemCount) {
                        mRecycler.addScrapView(child, position);
                    }
                }
            }
        }

        mMotionViewNewLeft = mMotionViewOriginalLeft + deltaX;

        mBlockLayoutRequests = true;

        if (count > 0) {
            detachViewsFromParent(start, count);
            mRecycler.removeSkippedScrap();
        }

        // invalidate before moving the children to avoid unnecessary invalidate
        // calls to bubble up from the children all the way to the top
        if (!awakenScrollBars()) {
            invalidate();
        }

        offsetChildrenLeftAndRight(incrementalDeltaX);

        if (rightTo) {
            mFirstPosition += count;
        }

        final int absIncrementalDeltaX = Math.abs(incrementalDeltaX);
        if (spacePrev < absIncrementalDeltaX
                || spaceNext < absIncrementalDeltaX) {
            fillGap(rightTo);
        }

        if (!inTouchMode && mSelectedPosition != INVALID_POSITION) {
            final int childIndex = mSelectedPosition - mFirstPosition;
            if (childIndex >= 0 && childIndex < getChildCount()) {
                positionSelector(mSelectedPosition, getChildAt(childIndex));
            }
        } else if (mSelectorPosition != INVALID_POSITION) {
            final int childIndex = mSelectorPosition - mFirstPosition;
            if (childIndex >= 0 && childIndex < getChildCount()) {
                positionSelector(INVALID_POSITION, getChildAt(childIndex));
            }
        } else {
            mSelectorRect.setEmpty();
        }

        mBlockLayoutRequests = false;

        invokeOnItemScrollListener();

        return false;
    }

    /**
     * Fills the gap left open by a touch-scroll. During a touch scroll,
     * children that remain on screen are shifted and the other ones are
     * discarded. The role of this method is to fill the gap thus created by
     * performing a partial layout in the empty space.
     * 
     * @param right
     *            true if the scroll is going right, false if it is going left
     */
    abstract void fillGap(boolean right);

    void hideSelector() {
        if (mSelectedPosition != INVALID_POSITION) {
            if (mLayoutMode != LAYOUT_SPECIFIC) {
                mResurrectToPosition = mSelectedPosition;
            }
            if (mNextSelectedPosition >= 0
                    && mNextSelectedPosition != mSelectedPosition) {
                mResurrectToPosition = mNextSelectedPosition;
            }
            setSelectedPositionInt(INVALID_POSITION);
            setNextSelectedPositionInt(INVALID_POSITION);
            mSelectedLeft = 0;
        }
    }

    /**
     * @return A position to select. First we try mSelectedPosition. If that has
     *         been clobbered by entering touch mode, we then try
     *         mResurrectToPosition. Values are pinned to the range of items
     *         available in the adapter
     */
    int reconcileSelectedPosition() {
        int position = mSelectedPosition;
        if (position < 0) {
            position = mResurrectToPosition;
        }
        position = Math.max(0, position);
        position = Math.min(position, mItemCount - 1);
        return position;
    }

    /**
     * Find the row closest to x. This row will be used as the motion row when
     * scrolling
     * 
     * @param x
     *            Where the user touched
     * @return The position of the first (or only) item in the row containing x
     */
    abstract int findMotionCol(int x);

    /**
     * Find the row closest to x. This row will be used as the motion row when
     * scrolling.
     * 
     * @param x
     *            Where the user touched
     * @return The position of the first (or only) item in the row closest to x
     */
    int findClosestMotionCol(int x) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return INVALID_POSITION;
        }

        final int motionCol = findMotionCol(x);
        return motionCol != INVALID_POSITION ? motionCol : mFirstPosition
                + childCount - 1;
    }

    /**
     * Causes all the views to be rebuilt and redrawn.
     */
    public void invalidateViews() {
        mDataChanged = true;
        rememberSyncState();
        requestLayout();
        invalidate();
    }

    /**
     * If there is a selection returns false. Otherwise resurrects the selection
     * and returns true if resurrected.
     */
    boolean resurrectSelectionIfNeeded() {
        if (mSelectedPosition < 0 && resurrectSelection()) {
            updateSelectorState();
            return true;
        }
        return false;
    }

    /**
     * Makes the item at the supplied position selected.
     * 
     * @param position
     *            the position of the new selection
     */
    abstract void setSelectionInt(int position);

    /**
     * Attempt to bring the selection back if the user is switching from touch
     * to trackball mode
     * 
     * @return Whether selection was set to something.
     */
    boolean resurrectSelection() {
        final int childCount = getChildCount();

        if (childCount <= 0) {
            return false;
        }

        int selectedLeft = 0;
        int selectedPos;
        int childrenLeft = mListPadding.left;
        int childrenRight = getRight() - getLeft() - mListPadding.bottom;
        final int firstPosition = mFirstPosition;
        final int toPosition = mResurrectToPosition;
        boolean rightTo = true;

        if (toPosition >= firstPosition
                && toPosition < firstPosition + childCount) {
            selectedPos = toPosition;

            final View selected = getChildAt(selectedPos - mFirstPosition);
            selectedLeft = selected.getLeft();
            int selectedRight = selected.getRight();

            // We are scrolled, don't get in the fade
            if (selectedLeft < childrenLeft) {
                selectedLeft = childrenLeft + getHorizontalFadingEdgeLength();
            } else if (selectedRight > childrenRight) {
                selectedLeft = childrenRight - selected.getMeasuredWidth()
                        - getHorizontalFadingEdgeLength();
            }
        } else {
            if (toPosition < firstPosition) {
                // Default to selecting whatever is first
                selectedPos = firstPosition;
                for (int i = 0; i < childCount; i++) {
                    final View v = getChildAt(i);
                    final int left = v.getLeft();

                    if (i == 0) {
                        // Remember the position of the first item
                        selectedLeft = left;
                        // See if we are scrolled at all
                        if (firstPosition > 0 || left < childrenLeft) {
                            // If we are scrolled, don't select anything that is
                            // in the fade region
                            childrenLeft += getHorizontalFadingEdgeLength();
                        }
                    }
                    if (left >= childrenLeft) {
                        // Found a view whose left is fully visisble
                        selectedPos = firstPosition + i;
                        selectedLeft = left;
                        break;
                    }
                }
            } else {
                final int itemCount = mItemCount;
                rightTo = false;
                selectedPos = firstPosition + childCount - 1;

                for (int i = childCount - 1; i >= 0; i--) {
                    final View v = getChildAt(i);
                    final int left = v.getLeft();
                    final int right = v.getRight();

                    if (i == childCount - 1) {
                        selectedLeft = left;
                        if (firstPosition + childCount < itemCount
                                || right > childrenRight) {
                            childrenRight -= getHorizontalFadingEdgeLength();
                        }
                    }

                    if (right <= childrenRight) {
                        selectedPos = firstPosition + i;
                        selectedLeft = left;
                        break;
                    }
                }
            }
        }

        mResurrectToPosition = INVALID_POSITION;
        removeCallbacks(mFlingRunnable);
        if (mPositionScroller != null) {
            mPositionScroller.stop();
        }
        mTouchMode = TOUCH_MODE_REST;
        clearScrollingCache();
        mSpecificLeft = selectedLeft;
        selectedPos = lookForSelectablePosition(selectedPos, rightTo);
        if (selectedPos >= firstPosition
                && selectedPos <= getLastVisiblePosition()) {
            mLayoutMode = LAYOUT_SPECIFIC;
            updateSelectorState();
            setSelectionInt(selectedPos);
            invokeOnItemScrollListener();
        } else {
            selectedPos = INVALID_POSITION;
        }
        reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);

        return selectedPos >= 0;
    }

    void confirmCheckedPositionsById() {
        // Clear out the positional check states, we'll rebuild it below from
        // IDs.
        mCheckStates.clear();

        boolean checkedCountChanged = false;
        int checkedIndex = 0;
        for (Map.Entry<Long, Integer> idEntry : mCheckedIdStates.entrySet()) {
            final long id = idEntry.getKey();
            final int lastPos = idEntry.getValue();

            final long lastPosId = mAdapter.getItemId(lastPos);
            if (id != lastPosId) {
                // Look around to see if the ID is nearby. If not, uncheck it.
                final int start = Math.max(0, lastPos
                        - CHECK_POSITION_SEARCH_DISTANCE);
                final int end = Math.min(lastPos
                        + CHECK_POSITION_SEARCH_DISTANCE, mItemCount);
                boolean found = false;
                for (int searchPos = start; searchPos < end; searchPos++) {
                    final long searchId = mAdapter.getItemId(searchPos);
                    if (id == searchId) {
                        found = true;
                        mCheckStates.put(searchPos, true);
                        mCheckedIdStates.put(Long.valueOf(checkedIndex),
                                searchPos);
                        break;
                    }
                }

                if (!found) {
                    mCheckedIdStates.remove(id);
                    checkedIndex--;
                    mCheckedItemCount--;
                    checkedCountChanged = true;
                    if (mChoiceActionMode != null
                            && mMultiChoiceModeCallback != null) {
                        mMultiChoiceModeCallback.onItemCheckedStateChanged(
                                mChoiceActionMode, lastPos, id, false);
                    }
                }
            } else {
                mCheckStates.put(lastPos, true);
            }

            checkedIndex++;
        }

        if (checkedCountChanged && mChoiceActionMode != null) {
            mChoiceActionMode.invalidate();
        }
    }

    /**
     * What is the distance between the source and destination rectangles given
     * the direction of focus navigation between them? The direction basically
     * helps figure out more quickly what is self evident by the relationship
     * between the rects...
     * 
     * @param source
     *            the source rectangle
     * @param dest
     *            the destination rectangle
     * @param direction
     *            the direction
     * @return the distance between the rectangles
     */
    static int getDistance(Rect source, Rect dest, int direction) {
        int sX, sY; // source x, y
        int dX, dY; // dest x, y
        switch (direction) {
        case View.FOCUS_RIGHT:
            sX = source.right;
            sY = source.top + source.height() / 2;
            dX = dest.left;
            dY = dest.top + dest.height() / 2;
            break;
        case View.FOCUS_DOWN:
            sX = source.left + source.width() / 2;
            sY = source.bottom;
            dX = dest.left + dest.width() / 2;
            dY = dest.top;
            break;
        case View.FOCUS_LEFT:
            sX = source.left;
            sY = source.top + source.height() / 2;
            dX = dest.right;
            dY = dest.top + dest.height() / 2;
            break;
        case View.FOCUS_UP:
            sX = source.left + source.width() / 2;
            sY = source.top;
            dX = dest.left + dest.width() / 2;
            dY = dest.bottom;
            break;
        case View.FOCUS_FORWARD:
        case View.FOCUS_BACKWARD:
            sX = source.right + source.width() / 2;
            sY = source.top + source.height() / 2;
            dX = dest.left + dest.width() / 2;
            dY = dest.top + dest.height() / 2;
            break;
        default:
            throw new IllegalArgumentException("direction must be one of "
                    + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
                    + "FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
        int deltaX = dX - sX;
        int deltaY = dY - sY;
        return deltaY * deltaY + deltaX * deltaX;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new AbsHorizontalListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(
            ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new AbsHorizontalListView.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof AbsHorizontalListView.LayoutParams;
    }

    /**
     * Puts the list or grid into transcript mode. In this mode the list or grid
     * will always scroll to the bottom to show new items.
     * 
     * @param mode
     *            the transcript mode to set
     * 
     * @see #TRANSCRIPT_MODE_DISABLED
     * @see #TRANSCRIPT_MODE_NORMAL
     * @see #TRANSCRIPT_MODE_ALWAYS_SCROLL
     */
    public void setTranscriptMode(int mode) {
        mTranscriptMode = mode;
    }

    /**
     * Returns the current transcript mode.
     * 
     * @return {@link #TRANSCRIPT_MODE_DISABLED},
     *         {@link #TRANSCRIPT_MODE_NORMAL} or
     *         {@link #TRANSCRIPT_MODE_ALWAYS_SCROLL}
     */
    public int getTranscriptMode() {
        return mTranscriptMode;
    }

    @Override
    public int getSolidColor() {
        return mCacheColorHint;
    }

    /**
     * When set to a non-zero value, the cache color hint indicates that this
     * list is always drawn on top of a solid, single-color, opaque background.
     * 
     * Zero means that what's behind this object is translucent (non solid) or
     * is not made of a single color. This hint will not affect any existing
     * background drawable set on this view ( typically set via
     * {@link #setBackgroundDrawable(Drawable)}).
     * 
     * @param color
     *            The background color
     */
    public void setCacheColorHint(int color) {
        if (color != mCacheColorHint) {
            mCacheColorHint = color;
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).setDrawingCacheBackgroundColor(color);
            }
            mRecycler.setCacheColorHint(color);
        }
    }

    /**
     * When set to a non-zero value, the cache color hint indicates that this
     * list is always drawn on top of a solid, single-color, opaque background
     * 
     * @return The cache color hint
     */
    @ViewDebug.ExportedProperty(category = "drawing")
    public int getCacheColorHint() {
        return mCacheColorHint;
    }

    /**
     * Move all views (excluding headers and footers) held by this
     * AbsHorizontalListView into the supplied List. This includes views
     * displayed on the screen as well as views stored in
     * AbsHorizontalListView's internal view recycler.
     * 
     * @param views
     *            A list into which to put the reclaimed views
     */
    public void reclaimViews(List<View> views) {
        int childCount = getChildCount();
        RecyclerListener listener = mRecycler.mRecyclerListener;

        // Reclaim views on screen
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            AbsHorizontalListView.LayoutParams lp = (AbsHorizontalListView.LayoutParams) child
                    .getLayoutParams();
            // Don't reclaim header or footer views, or views that should be
            // ignored
            if (lp != null && mRecycler.shouldRecycleViewType(lp.viewType)) {
                views.add(child);
                if (listener != null) {
                    // Pretend they went through the scrap heap
                    listener.onMovedToScrapHeap(child);
                }
            }
        }
        mRecycler.reclaimScrapViews(views);
        removeAllViewsInLayout();
    }

    /**
     * This defers a notifyDataSetChanged on the pending RemoteViewsAdapter if
     * it has not connected yet.
     */
    public void deferNotifyDataSetChanged() {
        mDeferNotifyDataSetChanged = true;
    }

    /**
     * Sets the recycler listener to be notified whenever a View is set aside in
     * the recycler for later reuse. This listener can be used to free resources
     * associated to the View.
     * 
     * @param listener
     *            The recycler listener to be notified of views set aside in the
     *            recycler.
     * 
     * @see android.widget.AbsHorizontalListView.RecycleBin
     * @see android.widget.AbsHorizontalListView.RecyclerListener
     */
    public void setRecyclerListener(RecyclerListener listener) {
        mRecycler.mRecyclerListener = listener;
    }

    /**
     * Offset the vertical location of all children of this view by the
     * specified number of pixels.
     * 
     * @param offset
     *            the number of pixels to offset
     * 
     * @hide
     */
    public void offsetChildrenLeftAndRight(int offset) {
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View v = getChildAt(i);
            mHideApiProxy.offsetLeftAndRight(v, offset);
        }
    }

    class AdapterDataSetObserver extends DataSetObserver {

        private Parcelable mInstanceState = null;

        @Override
        public void onChanged() {
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = getAdapter().getCount();

            // Detect the case where a cursor that was previously invalidated
            // has
            // been repopulated with new data.
            if (getAdapter().hasStableIds() && mInstanceState != null
                    && mOldItemCount == 0 && mItemCount > 0) {
                onRestoreInstanceState(mInstanceState);
                mInstanceState = null;
            } else {
                rememberSyncState();
            }
            checkFocus();
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            mDataChanged = true;

            if (getAdapter().hasStableIds()) {
                // Remember the current state for the case where our hosting
                // activity is being
                // stopped and later restarted
                mInstanceState = onSaveInstanceState();
            }

            // Data is invalid so we should reset our state
            mOldItemCount = mItemCount;
            mItemCount = 0;
            mSelectedPosition = INVALID_POSITION;
            mSelectedColId = INVALID_COL_ID;
            mNextSelectedPosition = INVALID_POSITION;
            mNextSelectedColId = INVALID_COL_ID;
            mNeedSync = false;

            checkFocus();
            requestLayout();
        }

        public void clearSavedState() {
            mInstanceState = null;
        }

    }

    /**
     * A MultiChoiceModeListener receives events for
     * {@link AbsHorizontalListView#CHOICE_MODE_MULTIPLE_MODAL}. It acts as the
     * {@link ActionMode.Callback} for the selection mode and also receives
     * {@link #onItemCheckedStateChanged(ActionMode, int, long, boolean)} events
     * when the user selects and deselects list items.
     */
    public interface MultiChoiceModeListener extends ActionMode.Callback {
        /**
         * Called when an item is checked or unchecked during selection mode.
         * 
         * @param mode
         *            The {@link ActionMode} providing the selection mode
         * @param position
         *            Adapter position of the item that was checked or unchecked
         * @param id
         *            Adapter ID of the item that was checked or unchecked
         * @param checked
         *            <code>true</code> if the item is now checked,
         *            <code>false</code> if the item is now unchecked.
         */
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                long id, boolean checked);
    }

    class MultiChoiceModeWrapper implements MultiChoiceModeListener {
        private MultiChoiceModeListener mWrapped;

        public void setWrapped(MultiChoiceModeListener wrapped) {
            mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mWrapped.onCreateActionMode(mode, menu)) {
                // Initialize checked graphic state?
                setLongClickable(false);
                return true;
            }
            return false;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mChoiceActionMode = null;

            // Ending selection mode means deselecting everything.
            clearChoices();

            mDataChanged = true;
            rememberSyncState();
            requestLayout();

            setLongClickable(true);
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position,
                long id, boolean checked) {
            mWrapped.onItemCheckedStateChanged(mode, position, id, checked);

            // If there are no items selected we no longer need the selection
            // mode.
            if (getCheckedItemCount() == 0) {
                mode.finish();
            }
        }
    }

    /**
     * AbsHorizontalListView extends LayoutParams to provide a place to hold the
     * view type.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * View type for this view, as returned by
         * {@link android.widget.Adapter#getItemViewType(int) }
         */
        @ViewDebug.ExportedProperty(category = "list", mapping = {
                @ViewDebug.IntToString(from = ITEM_VIEW_TYPE_IGNORE, to = "ITEM_VIEW_TYPE_IGNORE"),
                @ViewDebug.IntToString(from = ITEM_VIEW_TYPE_HEADER_OR_FOOTER, to = "ITEM_VIEW_TYPE_HEADER_OR_FOOTER") })
        int viewType;

        /**
         * When this boolean is set, the view has been added to the
         * AbsHorizontalListView at least once. It is used to know whether
         * headers/footers have already been added to the list view and whether
         * they should be treated as recycled views or not.
         */
        @ViewDebug.ExportedProperty(category = "list")
        boolean recycledHeaderFooter;

        /**
         * When an AbsHorizontalListView is measured with an AT_MOST measure
         * spec, it needs to obtain children views to measure itself. When doing
         * so, the children are not attached to the window, but put in the
         * recycler which assumes they've been attached before. Setting this
         * flag will force the reused view to be attached to the window rather
         * than just attached to the parent.
         */
        @ViewDebug.ExportedProperty(category = "list")
        boolean forceAdd;

        /**
         * The position the view was removed from when pulled out of the scrap
         * heap.
         * 
         * @hide
         */
        int scrappedFromPosition;

        /**
         * The ID the view represents
         */
        long itemId = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h);
            this.viewType = viewType;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    /**
     * A RecyclerListener is used to receive a notification whenever a View is
     * placed inside the RecycleBin's scrap heap. This listener is used to free
     * resources associated to Views placed in the RecycleBin.
     * 
     * @see android.widget.AbsHorizontalListView.RecycleBin
     * @see android.widget.AbsHorizontalListView#setRecyclerListener(android.widget.AbsHorizontalListView.RecyclerListener)
     */
    public static interface RecyclerListener {
        /**
         * Indicates that the specified View was moved into the recycler's scrap
         * heap. The view is not displayed on screen any more and any expensive
         * resource associated with the view should be discarded.
         * 
         * @param view
         */
        void onMovedToScrapHeap(View view);
    }

    /**
     * The RecycleBin facilitates reuse of views across layouts. The RecycleBin
     * has two levels of storage: ActiveViews and ScrapViews. ActiveViews are
     * those views which were onscreen at the start of a layout. By
     * construction, they are displaying current information. At the end of
     * layout, all views in ActiveViews are demoted to ScrapViews. ScrapViews
     * are old views that could potentially be used by the adapter to avoid
     * allocating views unnecessarily.
     * 
     * @see android.widget.AbsHorizontalListView#setRecyclerListener(android.widget.AbsHorizontalListView.RecyclerListener)
     * @see android.widget.AbsHorizontalListView.RecyclerListener
     */
    class RecycleBin {
        private RecyclerListener mRecyclerListener;

        /**
         * The position of the first view stored in mActiveViews.
         */
        private int mFirstActivePosition;

        /**
         * Views that were on screen at the start of layout. This array is
         * populated at the start of layout, and at the end of layout all view
         * in mActiveViews are moved to mScrapViews. Views in mActiveViews
         * represent a contiguous range of Views, with position of the first
         * view store in mFirstActivePosition.
         */
        private View[] mActiveViews = new View[0];

        /**
         * Unsorted views that can be used by the adapter as a convert view.
         */
        private ArrayList<View>[] mScrapViews;

        private int mViewTypeCount;

        private ArrayList<View> mCurrentScrap;

        private ArrayList<View> mSkippedScrap;

        private SparseArray<View> mTransientStateViews;

        public void setViewTypeCount(int viewTypeCount) {
            if (viewTypeCount < 1) {
                throw new IllegalArgumentException(
                        "Can't have a viewTypeCount < 1");
            }
            // noinspection unchecked
            ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++) {
                scrapViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mCurrentScrap = scrapViews[0];
            mScrapViews = scrapViews;
        }

        public void markChildrenDirty() {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    scrap.get(i).forceLayout();
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        scrap.get(j).forceLayout();
                    }
                }
            }
            if (mTransientStateViews != null) {
                final int count = mTransientStateViews.size();
                for (int i = 0; i < count; i++) {
                    mTransientStateViews.valueAt(i).forceLayout();
                }
            }
        }

        public boolean shouldRecycleViewType(int viewType) {
            return viewType >= 0;
        }

        /**
         * Clears the scrap heap.
         */
        void clear() {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    removeDetachedView(scrap.remove(scrapCount - 1 - i), false);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        removeDetachedView(scrap.remove(scrapCount - 1 - j),
                                false);
                    }
                }
            }
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        /**
         * Fill ActiveViews with all of the children of the
         * AbsHorizontalListView.
         * 
         * @param childCount
         *            The minimum number of views mActiveViews should hold
         * @param firstActivePosition
         *            The position of the first view that will be stored in
         *            mActiveViews
         */
        void fillActiveViews(int childCount, int firstActivePosition) {
            if (mActiveViews.length < childCount) {
                mActiveViews = new View[childCount];
            }
            mFirstActivePosition = firstActivePosition;

            final View[] activeViews = mActiveViews;
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                AbsHorizontalListView.LayoutParams lp = (AbsHorizontalListView.LayoutParams) child
                        .getLayoutParams();
                // Don't put header or footer views into the scrap heap
                if (lp != null
                        && lp.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                    // Note: We do place AdapterView.ITEM_VIEW_TYPE_IGNORE in
                    // active views.
                    // However, we will NOT place them into scrap views.
                    activeViews[i] = child;
                }
            }
        }

        /**
         * Get the view corresponding to the specified position. The view will
         * be removed from mActiveViews if it is found.
         * 
         * @param position
         *            The position to look up in mActiveViews
         * @return The view if it is found, null otherwise
         */
        View getActiveView(int position) {
            int index = position - mFirstActivePosition;
            final View[] activeViews = mActiveViews;
            if (index >= 0 && index < activeViews.length) {
                final View match = activeViews[index];
                activeViews[index] = null;
                return match;
            }
            return null;
        }

        View getTransientStateView(int position) {
            if (mTransientStateViews == null) {
                return null;
            }
            final int index = mTransientStateViews.indexOfKey(position);
            if (index < 0) {
                return null;
            }
            final View result = mTransientStateViews.valueAt(index);
            mTransientStateViews.removeAt(index);
            return result;
        }

        /**
         * Dump any currently saved views with transient state.
         */
        void clearTransientStateViews() {
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        /**
         * @return A view from the ScrapViews collection. These are unordered.
         */
        View getScrapView(int position) {
            if (mViewTypeCount == 1) {
                return retrieveFromScrap(mCurrentScrap, position);
            } else {
                int whichScrap = mAdapter.getItemViewType(position);
                if (whichScrap >= 0 && whichScrap < mScrapViews.length) {
                    return retrieveFromScrap(mScrapViews[whichScrap], position);
                }
            }
            return null;
        }

        /**
         * Put a view into the ScrapViews list. These views are unordered.
         * 
         * @param scrap
         *            The view to add
         */
        void addScrapView(View scrap, int position) {
            AbsHorizontalListView.LayoutParams lp = (AbsHorizontalListView.LayoutParams) scrap
                    .getLayoutParams();
            if (lp == null) {
                return;
            }

            lp.scrappedFromPosition = position;

            // Don't put header or footer views or views that should be ignored
            // into the scrap heap
            int viewType = lp.viewType;
            final boolean scrapHasTransientState = false; // scrap.hasTransientState();
            if (!shouldRecycleViewType(viewType) || scrapHasTransientState) {
                if (viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER
                        || scrapHasTransientState) {
                    if (mSkippedScrap == null) {
                        mSkippedScrap = new ArrayList<View>();
                    }
                    mSkippedScrap.add(scrap);
                }
                if (scrapHasTransientState) {
                    if (mTransientStateViews == null) {
                        mTransientStateViews = new SparseArray<View>();
                    }
                    mHideApiProxy.dispatchStartTemporaryDetach(scrap);
                    mTransientStateViews.put(position, scrap);
                }
                return;
            }

            mHideApiProxy.dispatchStartTemporaryDetach(scrap);
            if (mViewTypeCount == 1) {
                mCurrentScrap.add(scrap);
            } else {
                mScrapViews[viewType].add(scrap);
            }

            scrap.setAccessibilityDelegate(null);
            if (mRecyclerListener != null) {
                mRecyclerListener.onMovedToScrapHeap(scrap);
            }
        }

        /**
         * Finish the removal of any views that skipped the scrap heap.
         */
        void removeSkippedScrap() {
            if (mSkippedScrap == null) {
                return;
            }
            final int count = mSkippedScrap.size();
            for (int i = 0; i < count; i++) {
                removeDetachedView(mSkippedScrap.get(i), false);
            }
            mSkippedScrap.clear();
        }

        /**
         * Move all views remaining in mActiveViews to mScrapViews.
         */
        void scrapActiveViews() {
            final View[] activeViews = mActiveViews;
            final boolean hasListener = mRecyclerListener != null;
            final boolean multipleScraps = mViewTypeCount > 1;

            ArrayList<View> scrapViews = mCurrentScrap;
            final int count = activeViews.length;
            for (int i = count - 1; i >= 0; i--) {
                final View victim = activeViews[i];
                if (victim != null) {
                    final AbsHorizontalListView.LayoutParams lp = (AbsHorizontalListView.LayoutParams) victim
                            .getLayoutParams();
                    int whichScrap = lp.viewType;

                    activeViews[i] = null;

                    final boolean scrapHasTransientState = false; // victim.hasTransientState();
                    if (!shouldRecycleViewType(whichScrap)
                            || scrapHasTransientState) {
                        // Do not move views that should be ignored
                        if (whichScrap != ITEM_VIEW_TYPE_HEADER_OR_FOOTER
                                || scrapHasTransientState) {
                            removeDetachedView(victim, false);
                        }
                        if (scrapHasTransientState) {
                            if (mTransientStateViews == null) {
                                mTransientStateViews = new SparseArray<View>();
                            }
                            mTransientStateViews.put(mFirstActivePosition + i,
                                    victim);
                        }
                        continue;
                    }

                    if (multipleScraps) {
                        scrapViews = mScrapViews[whichScrap];
                    }
                    mHideApiProxy.dispatchStartTemporaryDetach(victim);
                    lp.scrappedFromPosition = mFirstActivePosition + i;
                    scrapViews.add(victim);

                    victim.setAccessibilityDelegate(null);
                    if (hasListener) {
                        mRecyclerListener.onMovedToScrapHeap(victim);
                    }
                }
            }

            pruneScrapViews();
        }

        /**
         * Makes sure that the size of mScrapViews does not exceed the size of
         * mActiveViews. (This can happen if an adapter does not recycle its
         * views).
         */
        private void pruneScrapViews() {
            final int maxViews = mActiveViews.length;
            final int viewTypeCount = mViewTypeCount;
            final ArrayList<View>[] scrapViews = mScrapViews;
            for (int i = 0; i < viewTypeCount; ++i) {
                final ArrayList<View> scrapPile = scrapViews[i];
                int size = scrapPile.size();
                final int extras = size - maxViews;
                size--;
                for (int j = 0; j < extras; j++) {
                    removeDetachedView(scrapPile.remove(size--), false);
                }
            }

            // if (mTransientStateViews != null) {
            // for (int i = 0; i < mTransientStateViews.size(); i++) {
            // final View v = mTransientStateViews.valueAt(i);
            // if (!v.hasTransientState()) {
            // mTransientStateViews.removeAt(i);
            // i--;
            // }
            // }
            // }
        }

        /**
         * Puts all views in the scrap heap into the supplied list.
         */
        void reclaimScrapViews(List<View> views) {
            if (mViewTypeCount == 1) {
                views.addAll(mCurrentScrap);
            } else {
                final int viewTypeCount = mViewTypeCount;
                final ArrayList<View>[] scrapViews = mScrapViews;
                for (int i = 0; i < viewTypeCount; ++i) {
                    final ArrayList<View> scrapPile = scrapViews[i];
                    views.addAll(scrapPile);
                }
            }
        }

        /**
         * Updates the cache color hint of all known views.
         * 
         * @param color
         *            The new cache color hint.
         */
        void setCacheColorHint(int color) {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    scrap.get(i).setDrawingCacheBackgroundColor(color);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        scrap.get(j).setDrawingCacheBackgroundColor(color);
                    }
                }
            }
            // Just in case this is called during a layout pass
            final View[] activeViews = mActiveViews;
            final int count = activeViews.length;
            for (int i = 0; i < count; ++i) {
                final View victim = activeViews[i];
                if (victim != null) {
                    victim.setDrawingCacheBackgroundColor(color);
                }
            }
        }
    }

    static View retrieveFromScrap(ArrayList<View> scrapViews, int position) {
        int size = scrapViews.size();
        if (size > 0) {
            // See if we still have a view for this position.
            for (int i = 0; i < size; i++) {
                View view = scrapViews.get(i);
                if (((AbsHorizontalListView.LayoutParams) view
                        .getLayoutParams()).scrappedFromPosition == position) {
                    scrapViews.remove(i);
                    return view;
                }
            }
            return scrapViews.remove(size - 1);
        } else {
            return null;
        }
    }
}
