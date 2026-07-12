package net.createmod.catnip.lang;

import java.nio.FloatBuffer;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Matrix4f;

import org.lwjgl.BufferUtils;

import net.createmod.catnip.platform.CatnipClientServices;
import net.createmod.catnip.render.PoseStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-side font wrapping that respects Minecraft's selected locale. */
@SideOnly(Side.CLIENT)
public final class ClientFontHelper {
    private ClientFontHelper() {
    }

    public static List<String> cutString(FontRenderer font, String text, int maxWidthPerLine) {
        if (font == null) throw new NullPointerException("font");
        if (text == null) throw new NullPointerException("text");
        if (maxWidthPerLine <= 0) throw new IllegalArgumentException("maxWidthPerLine must be positive");
        if (text.isEmpty()) return Collections.singletonList("");

        List<String> tokens = new ArrayList<String>();
        BreakIterator iterator = BreakIterator.getLineInstance(CatnipClientServices.CLIENT_HOOKS.getCurrentLocale());
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            tokens.add(text.substring(start, end));
        }

        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String token : tokens) {
            int segmentStart = 0;
            for (int i = 0; i <= token.length(); i++) {
                if (i != token.length() && token.charAt(i) != '\n') continue;
                appendSegment(font, token.substring(segmentStart, i), maxWidthPerLine, line, lines);
                if (i != token.length()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                segmentStart = i + 1;
            }
        }
        if (line.length() > 0 || lines.isEmpty() || text.endsWith("\n")) lines.add(line.toString());
        return lines;
    }

    private static void appendSegment(FontRenderer font, String segment, int maxWidth,
                                      StringBuilder line, List<String> lines) {
        String remaining = segment;
        while (!remaining.isEmpty()) {
            if (line.length() > 0 && font.getStringWidth(line.toString() + remaining) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (font.getStringWidth(remaining) <= maxWidth) {
                line.append(remaining);
                return;
            }

            int split = fittingPrefix(font, remaining, maxWidth);
            if (split <= 0) split = Character.charCount(remaining.codePointAt(0));
            line.append(remaining, 0, split);
            lines.add(line.toString());
            line.setLength(0);
            remaining = remaining.substring(split);
        }
    }

    private static int fittingPrefix(FontRenderer font, String text, int maxWidth) {
        int end = 0;
        int best = 0;
        while (end < text.length()) {
            end += Character.charCount(text.codePointAt(end));
            if (font.getStringWidth(text.substring(0, end)) > maxWidth) break;
            best = end;
        }
        return best;
    }

    public static void drawSplitString(PoseStack poseStack, FontRenderer font, String text,
                                       int x, int y, int width, int color) {
        if (poseStack == null) throw new NullPointerException("poseStack");
        List<String> lines = cutString(font, text, width);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.multMatrix(toBuffer(poseStack.last().pose()));
            drawLines(font, lines, x, y, width, color);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    public static void drawSplitString(FontRenderer font, String text, int x, int y, int width, int color) {
        drawLines(font, cutString(font, text, width), x, y, width, color);
    }

    private static void drawLines(FontRenderer font, List<String> lines, int x, int y, int width, int color) {
        for (String line : lines) {
            int drawX = x;
            if (font.getBidiFlag()) drawX += width - font.getStringWidth(line);
            font.drawString(line, drawX, y, color);
            y += font.FONT_HEIGHT;
        }
    }

    private static FloatBuffer toBuffer(Matrix4f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        buffer.put(matrix.m00).put(matrix.m10).put(matrix.m20).put(matrix.m30);
        buffer.put(matrix.m01).put(matrix.m11).put(matrix.m21).put(matrix.m31);
        buffer.put(matrix.m02).put(matrix.m12).put(matrix.m22).put(matrix.m32);
        buffer.put(matrix.m03).put(matrix.m13).put(matrix.m23).put(matrix.m33);
        buffer.flip();
        return buffer;
    }
}
