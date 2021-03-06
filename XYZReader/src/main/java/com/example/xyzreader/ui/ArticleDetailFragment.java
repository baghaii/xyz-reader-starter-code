package com.example.xyzreader.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
    LoaderManager.LoaderCallbacks<Cursor> {
  private static final String TAG = "ArticleDetailFragment";

  public static final String ARG_ITEM_ID = "item_id";
  private static final float PARALLAX_FACTOR = 1.25f;

  private Cursor mCursor;
  private long mItemId;
  private View mRootView;
  private View mContainer;
  private AppBarLayout mAppBarLayout;
  private GradientDrawable mGradientDrawable;
  private CollapsingToolbarLayout mCollapsingToolbarLayout;
  private Toolbar mToolbar;
  private int mMutedColor = 0xFF333333;
  private ColorDrawable mStatusBarColorDrawable;
  private FloatingActionButton mFab;

  private int mTopInset;
  private ImageView mPhotoView;
  private int mScrollY;
  private boolean mIsCard = false;
  private boolean mAppBarExpanded = true;
  private int mStatusBarFullOpacityBottom;

  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
  // Use default locale format
  private SimpleDateFormat outputFormat = new SimpleDateFormat();
  // Most time functions can only handle 1902 - 2037
  private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ArticleDetailFragment() {
  }

  public static ArticleDetailFragment newInstance(long itemId) {
    Bundle arguments = new Bundle();
    arguments.putLong(ARG_ITEM_ID, itemId);
    ArticleDetailFragment fragment = new ArticleDetailFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      mItemId = getArguments().getLong(ARG_ITEM_ID);
    }

    mIsCard = getResources().getBoolean(R.bool.detail_is_card);
    mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
        R.dimen.detail_card_top_margin);
    setHasOptionsMenu(true);
  }

  public ArticleDetailActivity getActivityCast() {
    return (ArticleDetailActivity) getActivity();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
    // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
    // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
    // we do this in onActivityCreated.
    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {


    mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
    mRootView.setVisibility(View.INVISIBLE);
    mContainer = mRootView.findViewById(R.id.story_container);
    mAppBarLayout = mRootView.findViewById(R.id.appbar);
    mCollapsingToolbarLayout = mRootView.findViewById(R.id.collapsing_toolbar);
    mToolbar = mRootView.findViewById(R.id.toolbar);
    mPhotoView = mRootView.findViewById(R.id.photo);
    mFab = mRootView.findViewById(R.id.share_fab);
    mStatusBarColorDrawable = new ColorDrawable(0);
    mRootView.findViewById(R.id.share_fab);
    mGradientDrawable = new GradientDrawable();
    mGradientDrawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);

    // How can I change the background on image collapsed?
    // https://blog.iamsuleiman.com/toolbar-animation-with-android-design-support-library/

    mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
      @Override
      public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        float mostlyCollapsedOffset = 0.75f * (getResources().getDimension(R.dimen.detail_photo_height)
            - getResources().getDimension(R.dimen.action_bar_height));

        if (Math.abs(verticalOffset) > mostlyCollapsedOffset) {
          mAppBarExpanded = false;
          mContainer.setBackground(mGradientDrawable);
        } else {
          mAppBarExpanded = true;
          mContainer.setBackgroundColor(getResources().getColor(R.color.midgray));
        }
      }
    });

    return mRootView;
  }



  private void updateStatusBar() {
    int color = 0;
    if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
      float f = progress(mScrollY,
          mStatusBarFullOpacityBottom - mTopInset * 3,
          mStatusBarFullOpacityBottom - mTopInset);
      color = Color.argb((int) (255 * f),
          (int) (Color.red(mMutedColor) * 0.9),
          (int) (Color.green(mMutedColor) * 0.9),
          (int) (Color.blue(mMutedColor) * 0.9));
    }

    mStatusBarColorDrawable.setColor(color);
  }

  static float progress(float v, float min, float max) {
    return constrain((v - min) / (max - min), 0, 1);
  }

  static float constrain(float val, float min, float max) {
    if (val < min) {
      return min;
    } else if (val > max) {
      return max;
    } else {
      return val;
    }
  }

  private Date parsePublishedDate() {
    try {
      String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
      return dateFormat.parse(date);
    } catch (ParseException ex) {
      Log.e(TAG, ex.getMessage());
      Log.i(TAG, "passing today's date");
      return new Date();
    }
  }

  private void bindViews() {
    if (mRootView == null) {
      return;
    }

    TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);

    TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
    bylineView.setMovementMethod(new LinkMovementMethod());
    TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
    bodyView.setMovementMethod(new ScrollingMovementMethod());

    String title = "";
    String author="";

    if (mCursor != null) {
      mRootView.setVisibility(View.VISIBLE);
      title = mCursor.getString(ArticleLoader.Query.TITLE);
      titleView.setText(title);
      mToolbar.setTitle(title);

      author = mCursor.getString(ArticleLoader.Query.AUTHOR);

      Configuration config = getResources().getConfiguration();
      int sw = config.smallestScreenWidthDp;

      //If this a tablet, make a longer title.
      //https://stackoverflow.com/questions/26521115/how-to-know-smallest-width-sw-of-an-android-device#36304657

      if (sw >= 600) {
        mToolbar.setTitle(title + " by " + author);
      }

      Date publishedDate = parsePublishedDate();
      if (!publishedDate.before(START_OF_EPOCH.getTime())) {
        bylineView.setText(Html.fromHtml(
            DateUtils.getRelativeTimeSpanString(
                publishedDate.getTime(),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString()
                + " by <font color='#ffffff'>"
                + author
                + "</font>"));

      } else {
        // If date is before 1902, just show the string
        bylineView.setText(Html.fromHtml(
            outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                + author
                + "</font>"));

      }

      /* Create an excerpt, not too big, to share in email */
      String body = mCursor.getString(ArticleLoader.Query.BODY);
      String email_excerpt;
      if (body.length() < 1000) {
        email_excerpt = body.substring(0, body.length());
      } else {
        email_excerpt = body.substring(0,1000).trim()+ "... " +
        "\nRead more at https://www.gutenberg.org/ebooks/search/?query=" +
            (title + "+" + author).replaceAll(" ","+");
      }

      makeFabAction(email_excerpt, title, author);


      bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
      ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
          .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
              Bitmap bitmap = imageContainer.getBitmap();
              if (bitmap != null) {
                Palette p = Palette.generate(bitmap, 12);
                mMutedColor = p.getDarkMutedColor(0xFF333333);
                mPhotoView.setImageBitmap(bitmap);
                mRootView.findViewById(R.id.meta_bar)
                    .setBackgroundColor(mMutedColor);
                mCollapsingToolbarLayout.setContentScrimColor(mMutedColor);
                mGradientDrawable.setColors(new int[]{
                    getResources().getColor(R.color.midgray),
                    mMutedColor
                });
              }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
          });
    } else {
      mRootView.setVisibility(View.GONE);
      mToolbar.setTitle("N/A");
      titleView.setText("N/A");
      bylineView.setText("N/A");
      bodyView.setText("N/A");
    }
  }

  @Override
  @NonNull
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
    if (!isAdded()) {
      if (cursor != null) {
        cursor.close();
      }
      return;
    }

    mCursor = cursor;
    if (mCursor != null && !mCursor.moveToFirst()) {
      Log.e(TAG, "Error reading item detail cursor");
      mCursor.close();
      mCursor = null;
    }

    bindViews();

  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
    mCursor = null;
    bindViews();
  }

  public void makeFabAction(final String email, final String title, final String author) {
    final String emailTitle = title + " by " + author;

    mFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
            .setType("text/plain")
            .setSubject(emailTitle)
            .setText(email)
            .getIntent(), getString(R.string.action_share)));
      }
    });
  }

}
