/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.reddit.prepared;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BaseActivity;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.common.ScreenreaderPronunciation;
import org.quantumbadger.redreader.common.StringUtils;
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction;
import org.quantumbadger.redreader.reddit.things.RedditComment;
import org.quantumbadger.redreader.reddit.things.RedditThingWithIdAndType;

import java.net.URI;

public class RedditRenderableComment
		implements RedditRenderableInboxItem, RedditThingWithIdAndType {

	private final RedditParsedComment mComment;
	@Nullable private final String mParentPostAuthor;
	private final Integer mMinimumCommentScore;
	private final String mCurrentCanonicalUserName;
	private final boolean mShowScore;
	private final boolean mShowSubreddit;
	private final boolean mNeverAutoCollapse;

	public final static int NO_TIMESTAMP = -1;

	public RedditRenderableComment(
			final RedditParsedComment comment,
			@Nullable final String parentPostAuthor,
			final Integer minimumCommentScore,
			final String currentCanonicalUserName,
			final boolean showScore,
			final boolean showSubreddit,
			final boolean neverAutoCollapse) {

		mComment = comment;
		mParentPostAuthor = parentPostAuthor;
		mMinimumCommentScore = minimumCommentScore;
		mCurrentCanonicalUserName = currentCanonicalUserName;
		mShowScore = showScore;
		mShowSubreddit = showSubreddit;
		mNeverAutoCollapse = neverAutoCollapse;
	}

	private int computeScore(final RedditChangeDataManager changeDataManager) {

		final RedditComment rawComment = mComment.getRawComment();

		int score = rawComment.ups - rawComment.downs;

		if(Boolean.TRUE.equals(rawComment.likes)) {
			score--;
		}
		if(Boolean.FALSE.equals(rawComment.likes)) {
			score++;
		}

		if(changeDataManager.isUpvoted(mComment)) {
			score++;
		} else if(changeDataManager.isDownvoted(mComment)) {
			score--;
		}

		return score;
	}

	@Override
	public CharSequence getHeader(
			final RRThemeAttributes theme,
			final RedditChangeDataManager changeDataManager,
			final Context context,
			final int commentAgeUnits,
			final long postCreated,
			final long parentCommentCreated) {

		final PrefsUtility.CommentAgeMode commentAgeMode
				= PrefsUtility.appearance_comment_age_mode();

		final BetterSSB sb = new BetterSSB();

		final RedditComment rawComment = mComment.getRawComment();

		final int pointsCol;
		final int score = computeScore(changeDataManager);

		if(changeDataManager.isUpvoted(mComment)) {
			pointsCol = theme.rrPostSubtitleUpvoteCol;

		} else if(changeDataManager.isDownvoted(mComment)) {
			pointsCol = theme.rrPostSubtitleDownvoteCol;

		} else {
			pointsCol = theme.rrCommentHeaderBoldCol;
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.AUTHOR)) {

			boolean setBackgroundColour = false;
			int backgroundColour = 0; // TODO color from theme

			if(rawComment.author.equalsIgnoreCase(mParentPostAuthor)
					&& !rawComment.author.equals("[deleted]")) {

				setBackgroundColour = true;
				backgroundColour = Color.rgb(0, 126, 168);

			} else if("moderator".equals(rawComment.distinguished)) {
				setBackgroundColour = true;
				backgroundColour = Color.rgb(0, 170, 0);

			} else if("admin".equals(rawComment.distinguished)) {
				setBackgroundColour = true;
				backgroundColour = Color.rgb(170, 0, 0);
			}

			if(setBackgroundColour) {

				sb.append(
						" " + rawComment.author + " ",
						BetterSSB.BACKGROUND_COLOR
								| BetterSSB.FOREGROUND_COLOR
								| BetterSSB.BOLD,
						Color.WHITE,
						backgroundColour,
						1f);

			} else {
				sb.append(
						rawComment.author,
						BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD,
						theme.rrCommentHeaderAuthorCol,
						0,
						1f);
			}
		}

		final String flair = mComment.getFlair();

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.FLAIR)
				&& flair != null && !flair.isEmpty()) {

			if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.AUTHOR)) {
				sb.append("  ", 0);
			}

			sb.append(
					" " + flair + General.LTR_OVERRIDE_MARK + " ",
					BetterSSB.FOREGROUND_COLOR | BetterSSB.BACKGROUND_COLOR,
					theme.rrFlairTextCol,
					theme.rrFlairBackCol,
					1f);
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.AUTHOR)
				|| theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.FLAIR)) {
			sb.append("   ", 0);
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.SCORE)
				&& mShowScore) {

			if(!Boolean.TRUE.equals(rawComment.score_hidden)) {
				sb.append(
						String.valueOf(score),
						BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD,
						pointsCol,
						0,
						1f);
			} else {
				sb.append(
						"??",
						BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD,
						pointsCol,
						0,
						1f);
			}

			sb.append(" " + context.getString(R.string.subtitle_points), 0);

			if(!theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.CONTROVERSIALITY)) {
				sb.append(" ", 0);
			}
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.CONTROVERSIALITY)) {

			if(rawComment.isControversial()) {
				sb.append(
						context.getString(R.string.props_controversial_symbol),
						BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD | BetterSSB.SUPERSCRIPT,
						theme.rrCommentHeaderBoldCol,
						0,
						1f);
			}

			sb.append(" ", 0);
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.GOLD)) {

			if(rawComment.gilded > 0) {

				sb.append(" ", 0);

				sb.append(
						" "
								+ context.getString(R.string.gold)
								+ BetterSSB.NBSP + "x"
								+ rawComment.gilded
								+ " ",
						BetterSSB.FOREGROUND_COLOR | BetterSSB.BACKGROUND_COLOR,
						theme.rrGoldTextCol,
						theme.rrGoldBackCol,
						1f);

				sb.append("  ", 0);
			}
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.AGE)) {
			final long commentTime = rawComment.created_utc * 1000L;
			final String formattedAge = formatAge(
					context,
					commentAgeMode,
					commentAgeUnits,
					commentTime,
					postCreated,
					parentCommentCreated);

			sb.append(
					formattedAge,
					BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD,
					theme.rrCommentHeaderBoldCol,
					0,
					1f);

			if(rawComment.wasEdited()) {
				sb.append(
						"*",
						BetterSSB.FOREGROUND_COLOR | BetterSSB.BOLD,
						theme.rrCommentHeaderBoldCol,
						0,
						1f);
			}

			sb.append(" ", 0);
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.SUBREDDIT)
				&& mShowSubreddit) {
			sb.append(context.getString(R.string.subtitle_to) + " ", 0);

			sb.append(
					mComment.getRawComment().subreddit,
					BetterSSB.BOLD | BetterSSB.FOREGROUND_COLOR,
					theme.rrCommentHeaderBoldCol,
					0,
					1f);
		}

		return sb.get();
	}

	@Override
	public String getAccessibilityHeader(
			final RRThemeAttributes theme,
			final RedditChangeDataManager changeDataManager,
			final Context context,
			final int commentAgeUnits,
			final long postCreated,
			final long parentCommentCreated,
			final boolean collapsed) {

		final PrefsUtility.CommentAgeMode commentAgeMode
				= PrefsUtility.appearance_comment_age_mode();

		final StringBuilder accessibilityHeader = new StringBuilder();

		final RedditComment rawComment = mComment.getRawComment();

		final String separator = " \n";

		if(collapsed) {
			accessibilityHeader
					.append(context.getString(R.string.accessibility_subtitle_comment_collapsed))
					.append(separator);
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.AUTHOR)) {
			@StringRes final int authorString;

			if(rawComment.author.equalsIgnoreCase(mParentPostAuthor)
					&& !rawComment.author.equals("[deleted]")) {
				if("moderator".equals(rawComment.distinguished)) {
					authorString
							= R.string.accessibility_subtitle_author_submitter_moderator_withperiod;
				} else if("admin".equals(rawComment.distinguished)) {
					authorString
							= R.string.accessibility_subtitle_author_submitter_admin_withperiod;
				} else {
					authorString = R.string.accessibility_subtitle_author_submitter_withperiod;
				}
			} else {
				if("moderator".equals(rawComment.distinguished)) {
					authorString = R.string.accessibility_subtitle_author_moderator_withperiod;
				} else if("admin".equals(rawComment.distinguished)) {
					authorString = R.string.accessibility_subtitle_author_admin_withperiod;
				} else {
					authorString = R.string.accessibility_subtitle_author_withperiod;
				}
			}

			accessibilityHeader
					.append(context.getString(
							authorString,
							ScreenreaderPronunciation.getPronunciation(
									context,
									rawComment.author)))
					.append(separator);
		}

		final String flair = mComment.getFlair();

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.FLAIR)
				&& flair != null
				&& !flair.isEmpty()) {

			accessibilityHeader
					.append(context.getString(
							R.string.accessibility_subtitle_flair_withperiod,
							flair + General.LTR_OVERRIDE_MARK))
					.append(separator);
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.SCORE) && mShowScore) {

			if(Boolean.TRUE.equals(rawComment.score_hidden)) {
				accessibilityHeader
						.append(context.getString(
								R.string.accessibility_subtitle_points_unknown_withperiod))
						.append(separator);

			} else {
				accessibilityHeader
						.append(context.getString(
								R.string.accessibility_subtitle_points_withperiod,
								computeScore(changeDataManager)))
						.append(separator);
			}
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.CONTROVERSIALITY)) {

			if(rawComment.isControversial()) {
				accessibilityHeader
						.append(context.getString(
								R.string.accessibility_subtitle_controversiality_withperiod))
						.append(separator);
			}
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.GOLD)) {

			if(rawComment.gilded > 0) {
				accessibilityHeader
						.append(context.getString(
								R.string.accessibility_subtitle_gold_withperiod,
								rawComment.gilded))
						.append(separator);
			}
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.AGE)) {
			final long commentTime = rawComment.created_utc * 1000L;
			final String formattedAge = formatAge(
					context,
					commentAgeMode,
					commentAgeUnits,
					commentTime,
					postCreated,
					parentCommentCreated);

			accessibilityHeader
					.append(context.getString(
							R.string.accessibility_subtitle_age_withperiod,
							formattedAge))
					.append(separator);

			if(rawComment.wasEdited()) {
				accessibilityHeader
						.append(context.getString(
								R.string.accessibility_subtitle_edited_since_being_posted))
						.append(separator);
			}
		}

		if(theme.shouldShow(PrefsUtility.AppearanceCommentHeaderItem.SUBREDDIT)
				&& mShowSubreddit) {

			accessibilityHeader
					.append(context.getString(
							R.string.accessibility_subtitle_subreddit_withperiod,
							ScreenreaderPronunciation.getPronunciation(
									context,
									mComment.getRawComment().subreddit)))
					.append(separator);
		}

		return accessibilityHeader.toString();
	}

	@NonNull
	private String formatAge(
			@NonNull final Context context,
			@NonNull final PrefsUtility.CommentAgeMode commentAgeMode,
			final int commentAgeUnits,
			final long commentTime,
			final long postCreated,
			final long parentCommentCreated) {
		//In addition to enforcing the user's prefs, the lower mode cases also act as fallbacks
		switch(commentAgeMode) {
			case RELATIVE_PARENT:
				if(parentCommentCreated != NO_TIMESTAMP) {
					return RRTime.formatDurationFrom(
							context,
							parentCommentCreated * 1000L,
							commentTime,
							R.string.time_after_reply,
							commentAgeUnits);
				}
			//Top-level comment (or unable to get the parent comment's creation time)
			case RELATIVE_POST:
				if(postCreated != NO_TIMESTAMP) {
					return RRTime.formatDurationFrom(
							context,
							postCreated * 1000L,
							commentTime,
							R.string.time_after,
							commentAgeUnits);
				}
			//Unable to get post creation date, resorting to absolute age
			case ABSOLUTE:
				return RRTime.formatDurationFrom(
						context,
						commentTime,
						R.string.time_ago,
						commentAgeUnits);
			default:
				throw new IllegalStateException("Unexpected value: " + commentAgeMode);
		}
	}

	@Override
	public View getBody(
			final BaseActivity activity,
			final Integer textColor,
			final Float textSize,
			final boolean showLinkButtons) {

		return mComment.getBody()
				.generateView(activity, textColor, textSize, showLinkButtons);
	}

	@Override
	public void handleInboxClick(final BaseActivity activity) {
		final URI commentContext
				= Constants.Reddit.getUri(mComment.getRawComment().context);
		LinkHandler.onLinkClicked(activity, commentContext.toString());
	}

	@Override
	public void handleInboxLongClick(final BaseActivity activity) {

		final RedditChangeDataManager changeDataManager
				= RedditChangeDataManager.getInstance(
				RedditAccountManager.getInstance(activity).getDefaultAccount());

		RedditAPICommentAction.showActionMenu(
				activity,
				null,
				this,
				null,
				changeDataManager,
				// TODO instead of assuming that it isn't an archived post,
				//  somehow find out if it actually is
				false);
	}

	@Override
	public String getIdAlone() {
		return mComment.getIdAlone();
	}

	@Override
	public String getIdAndType() {
		return mComment.getIdAndType();
	}

	public RedditParsedComment getParsedComment() {
		return mComment;
	}

	private boolean isScoreBelowThreshold(final RedditChangeDataManager changeDataManager) {

		if(mMinimumCommentScore == null) {
			return false;
		}

		if(Boolean.TRUE.equals(mComment.getRawComment().score_hidden)) {
			return false;
		}

		return (computeScore(changeDataManager) < mMinimumCommentScore);
	}

	public boolean isCollapsed(final RedditChangeDataManager changeDataManager) {

		final Boolean collapsed = changeDataManager.isHidden(this);

		if(collapsed != null) {
			return collapsed;
		}

		if(mNeverAutoCollapse) {
			return false;
		}

		if(StringUtils.asciiLowercase(mComment.getRawComment().author.trim())
				.equals(mCurrentCanonicalUserName)) {
			return false;
		}

		return isScoreBelowThreshold(changeDataManager);

	}
}
