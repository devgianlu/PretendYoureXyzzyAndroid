package com.gianlu.pretendyourexyzzy.CardViews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.typography.FontsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.GameRound;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.RoundCard;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.List;
import java.util.Random;

public class GameRoundSummary {
    private final static int PADDING = 48;
    private final static int CARD_HEIGHT = 512;
    private final static int CARD_WIDTH = 348;
    private final static int MAX_COLUMNS = 6; // Must be even
    private final static int CARD_RADIUS = 24;
    private final static int CARD_INTERNAL_PADDING = 36;
    private final static int MAX_TEXT_SIZE = 64;
    private final static int SHADOW_DX = -18;
    private final static int SHADOW_DY = 18;
    private final static int SHADOW_RADIUS = 4;
    private final static int SHADOW_COLOR = 0x80B2B2B2;
    private final static int BACKGROUND_COLOR = 0xFFE5E5E5;
    private final static int WINNER_COLOR = 0xFF4CC7FF;
    private final static int MAX_ROTATION = 10;
    private final static int WATERMARK_TEXT_SIZE = 48;
    private final GameRound round;
    private final boolean rotate;
    private final Bitmap bitmap;
    private final Canvas canvas;
    private final Paint blackPaint;
    private final Paint whitePaint;
    private final Paint winnerPaint;
    private final TextPaint whiteTextPaint;
    private final TextPaint blackTextPaint;
    private final Paint backgroundPaint;
    private final Paint boxPaint;
    private final Random random = new Random();
    private final TextPaint grayTextPaint;
    private int rows;
    private int cols;

    public GameRoundSummary(@NonNull Context context, @NonNull GameRound round, boolean rotate) {
        this.round = round;
        this.rotate = rotate;

        blackPaint = new Paint();
        blackPaint.setColor(Color.BLACK);
        blackPaint.setAntiAlias(true);
        blackPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR);

        blackTextPaint = new TextPaint();
        blackTextPaint.setAntiAlias(true);
        blackTextPaint.setColor(Color.BLACK);
        FontsManager.set(context, blackTextPaint, FontsManager.ROBOTO_MEDIUM);

        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setAntiAlias(true);
        whitePaint.setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR);

        whiteTextPaint = new TextPaint();
        whiteTextPaint.setColor(Color.WHITE);
        whiteTextPaint.setAntiAlias(true);
        FontsManager.set(context, whiteTextPaint, FontsManager.ROBOTO_MEDIUM);

        winnerPaint = new Paint();
        winnerPaint.setColor(WINNER_COLOR);
        winnerPaint.setAntiAlias(true);
        winnerPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR);

        grayTextPaint = new TextPaint();
        grayTextPaint.setColor(Color.DKGRAY);
        grayTextPaint.setAntiAlias(true);
        FontsManager.set(context, grayTextPaint, FontsManager.ROBOTO_MEDIUM);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(BACKGROUND_COLOR);

        boxPaint = new Paint();
        boxPaint.setColor(Color.GRAY);

        bitmap = Bitmap.createBitmap(measureWidth(), measureHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        draw();

        ThisApplication.sendAnalytics(Utils.ACTION_SHOW_ROUND);
    }

    private int measureHeight() {
        rows = (int) Math.ceil((float) round.whiteCards() / (float) MAX_COLUMNS);
        return PADDING * (rows + 1) + CARD_HEIGHT * rows;
    }

    private int measureWidth() {
        int whites = round.whiteCards();
        cols = Math.min(MAX_COLUMNS, whites);
        cols++; // Black card
        return PADDING * (cols + 1) + CARD_WIDTH * cols;
    }

    private void drawCard(int x, int y, boolean winner, @NonNull RoundCard card) {
        Paint paint;
        TextPaint textPaint;
        TextPaint watermarkPaint;
        if (winner) {
            paint = winnerPaint;
            textPaint = whiteTextPaint;
            watermarkPaint = whiteTextPaint;
        } else {
            paint = card.black() ? blackPaint : whitePaint;
            textPaint = card.black() ? whiteTextPaint : blackTextPaint;
            watermarkPaint = card.black() ? whiteTextPaint : grayTextPaint;
        }

        RectF rect = new RectF(x, y, x + CARD_WIDTH, y + CARD_HEIGHT);
        int maxTextHeight = (int) (rect.height() - CARD_INTERNAL_PADDING * 2);
        int maxTextWidth = (int) (rect.width() - CARD_INTERNAL_PADDING * 2);

        canvas.save();
        if (rotate)
            canvas.rotate(-MAX_ROTATION + random.nextFloat() * MAX_ROTATION * 2, rect.centerX(), rect.centerY());

        canvas.drawRoundRect(rect, CARD_RADIUS, CARD_RADIUS, paint);

        StaticLayout text;

        canvas.save();
        watermarkPaint.setTextSize(WATERMARK_TEXT_SIZE);
        text = new StaticLayout(card.watermark, watermarkPaint, maxTextWidth, Layout.Alignment.ALIGN_OPPOSITE, 1, 0, true);
        maxTextHeight -= text.getHeight();
        canvas.translate(rect.left + CARD_INTERNAL_PADDING, rect.top + maxTextHeight + CARD_INTERNAL_PADDING);
        text.draw(canvas);
        canvas.restore();

        textPaint.setTextSize(MAX_TEXT_SIZE);
        do {
            text = new StaticLayout(card.text(), textPaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
            textPaint.setTextSize(textPaint.getTextSize() - 1);
        } while (text.getHeight() >= maxTextHeight);

        canvas.translate(rect.left + CARD_INTERNAL_PADDING, rect.top + CARD_INTERNAL_PADDING);
        text.draw(canvas);
        canvas.restore();
    }

    private int drawCards(int x, int y, boolean winner, List<RoundCard> cards) {
        if (cards.size() == 1) {
            drawCard(x + PADDING, y + PADDING, winner, cards.get(0));
        } else {
            RectF rect = new RectF(x + CARD_INTERNAL_PADDING, y + CARD_INTERNAL_PADDING, x + (CARD_WIDTH + PADDING) * cards.size(), y + CARD_HEIGHT + CARD_INTERNAL_PADDING);
            canvas.drawRoundRect(rect, CARD_RADIUS, CARD_RADIUS, boxPaint);

            for (int i = 0; i < cards.size(); i++)
                drawCard(x + PADDING * (i + 1) + CARD_WIDTH * i, y + PADDING, winner, cards.get(i));
        }

        return cards.size();
    }

    private void draw() {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        drawCard(PADDING, PADDING, false, round.blackCard);
        drawCards(PADDING + CARD_WIDTH, 0, true, round.winningCard);

        int i = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = row == 0 ? (1 + round.winningCard.size()) : 1; col < cols; ) {
                if (i < round.otherCards.size()) {
                    col += drawCards((PADDING + CARD_WIDTH) * col, (PADDING + CARD_HEIGHT) * row, false, round.otherCards.get(i));
                    i++;
                } else {
                    break;
                }
            }
        }
    }

    @NonNull
    public Bitmap getBitmap() {
        return bitmap;
    }

    @NonNull
    public String getName() {
        return "PYX GameRound - " + round.gameId + " - " + round.timestamp;
    }
}
