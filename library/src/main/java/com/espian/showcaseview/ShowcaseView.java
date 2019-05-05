package com.espian.showcaseview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * A view which allows you to showcase areas of your app with an explanation.
 */
public class ShowcaseView extends RelativeLayout implements View.OnClickListener, View.OnTouchListener {

	public static final int TYPE_NO_LIMIT = 0;
	public static final int TYPE_ONE_SHOT = 1;

	private static final String INTERNAL_PREFS = "showcase_internal";
	private static final String SHOT_PREF_STORE = "wasShot";
	private static final int DEFAULT_BG_COLOR = Color.argb(128, 80, 80, 80);

	private float showcaseX = -1;
	private float showcaseY = -1;
	private int shotType = TYPE_NO_LIMIT;
	private boolean isRedundant = false;
	private boolean block = true;
	private float showcaseRadius = -1;

	private Canvas mDispatchCanvas = new Canvas();
	private Paint background;
	private int mBackColor;
	private Drawable showcase;
	private View mButton;
	private OnClickListener mClickListener;
	private OnShowcaseEventListener mEventListener;

	public ShowcaseView(Context context) {
		this(context, null, 0);
	}

	public ShowcaseView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ShowcaseView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (attrs != null) {
			TypedArray styled = getContext().obtainStyledAttributes(attrs, R.styleable.ShowcaseView, defStyle, 0);
			mBackColor = styled.getInt(R.styleable.ShowcaseView_backgroundColor, DEFAULT_BG_COLOR);
			styled.recycle();
		}
	}

	private void init() {
		boolean hasShot = getContext().getSharedPreferences(INTERNAL_PREFS, Context.MODE_PRIVATE).getBoolean(
				SHOT_PREF_STORE, false);
		if (hasShot && shotType == TYPE_ONE_SHOT) {
			// The showcase has already been shot once, so we don't need to do
			// anything
			setVisibility(View.GONE);
			isRedundant = true;
			return;
		}
		background = new Paint();
		background.setColor(mBackColor);
		showcase = getContext().getResources().getDrawable(R.drawable.cling);
		mButton = findViewById(R.id.showcase_button);
		if (mButton != null) {
			mButton.setOnClickListener(this);
		}
		float dens = getResources().getDisplayMetrics().density;
		showcaseRadius = dens * 94;
		setOnTouchListener(this);
	}

	/**
	 * Set the view to showcase.
	 * 
	 * @param view
	 *            The {@link View} to showcase.
	 */
	public void setShowcaseView(final View view) {
		if (isRedundant || view == null) {
			isRedundant = true;
			return;
		}
		isRedundant = false;

		view.post(new Runnable() {
			@Override
			public void run() {
				init();
				showcaseX = (float) (view.getLeft() + view.getWidth() / 2);
				showcaseY = (float) (view.getTop() + view.getHeight() / 2);
				invalidate();
			}
		});
	}

	/**
	 * Set a specific position to showcase
	 * 
	 * @param x
	 * @param y
	 */
	public void setShowcasePosition(float x, float y) {
		if (isRedundant) {
			return;
		}
		showcaseX = x;
		showcaseY = y;
		invalidate();
	}

	/**
	 * Set the shot method of the showcase - only once or no limit.
	 * 
	 * @param shotType
	 *            either TYPE_ONE_SHOT or TYPE_NO_LIMIT
	 */
	public void setShotType(int shotType) {
		if (shotType == TYPE_NO_LIMIT || shotType == TYPE_ONE_SHOT) {
			this.shotType = shotType;
		}
	}

	/**
	 * Decide whether touches outside the showcased circle should be ignored or
	 * not
	 * 
	 * @param block
	 *            true to block touches, false otherwise. By default, this is
	 *            true.
	 */
	public void blockNonShowcasedTouches(boolean block) {
		this.block = block;
	}

	/**
	 * Override the standard button click event, if there is a button available
	 * 
	 * @param listener
	 *            Listener to listen to on click events
	 */
	public void overrideButtonClick(OnClickListener listener) {
		if (isRedundant) {
			return;
		}
		if (mButton != null) {
			mButton.setOnClickListener(listener);
		}
	}

	public void setOnShowcaseEventListener(OnShowcaseEventListener listener) {
		mEventListener = listener;
	}

	public void removeOnShowcaseEventListener() {
		setOnClickListener(null);
	}

	@Override
	public void dispatchDraw(Canvas canvas) {
		if (showcaseX < 0 || showcaseY < 0 || isRedundant) {
			super.dispatchDraw(canvas);
			return;
		}

		Bitmap b = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		mDispatchCanvas.setBitmap(b);

		// Draw the semi-transparent background
		mDispatchCanvas.drawColor(mBackColor);

		// Erase the area for the ring
		Paint eraser = new Paint();
		eraser.setColor(0xFFFFFF);
		eraser.setAlpha(0);
		eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
		mDispatchCanvas.drawCircle(showcaseX, showcaseY, showcaseRadius, eraser);

		int cx = (int) showcaseX, cy = (int) showcaseY;
		int dw = showcase.getIntrinsicWidth();
		int dh = showcase.getIntrinsicHeight();

		showcase.setBounds(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2);
		showcase.draw(mDispatchCanvas);

		canvas.drawBitmap(b, 0, 0, null);
		b.recycle();
		super.dispatchDraw(canvas);

	}

	@Override
	public void onClick(View view) {
		// If the type is set to one-shot, store that it has shot
		if (shotType == TYPE_ONE_SHOT) {
			SharedPreferences internal = getContext().getSharedPreferences(INTERNAL_PREFS, Context.MODE_PRIVATE);
			internal.edit().putBoolean(SHOT_PREF_STORE, true).commit();
		}
		if (mClickListener == null) {
			hide();
		} else {
			mClickListener.onClick(view);
		}
	}

	@TargetApi(11)
	public void hide() {
		if (mEventListener != null) {
			mEventListener.onShowcaseViewHide(this);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ObjectAnimator oa = ObjectAnimator.ofFloat(this, "alpha", 0f);
			oa.setDuration(300).addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animator) {
				}

				@Override
				public void onAnimationEnd(Animator animator) {
					setVisibility(View.GONE);
				}

				@Override
				public void onAnimationCancel(Animator animator) {
				}

				@Override
				public void onAnimationRepeat(Animator animator) {
				}
			});
			oa.start();
		} else {
			setVisibility(View.GONE);
		}
	}

	/**
	 * Show the showcase. This will trigger the onShowcaseViewShow for
	 * interested observers.
	 * 
	 */
	@TargetApi(11)
	public void show() {
		if (mEventListener != null) {
			mEventListener.onShowcaseViewShow(this);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ObjectAnimator oa = ObjectAnimator.ofFloat(this, "alpha", 1f);
			oa.setDuration(300).addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animator) {
					setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationEnd(Animator animator) {
				}

				@Override
				public void onAnimationCancel(Animator animator) {
				}

				@Override
				public void onAnimationRepeat(Animator animator) {
				}
			});
			oa.start();
		} else {
			setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		if (!block) {
			return false;
		} else {
			float xDelta = Math.abs(motionEvent.getRawX() - showcaseX);
			float yDelta = Math.abs(motionEvent.getRawY() - showcaseY);
			double distanceFromFocus = Math.sqrt(Math.pow(xDelta, 2) + Math.pow(yDelta, 2));
			return distanceFromFocus > showcaseRadius;
		}
	}

	public interface OnShowcaseEventListener {

		public void onShowcaseViewHide(ShowcaseView showcaseView);

		public void onShowcaseViewShow(ShowcaseView showcaseView);

	}

}
