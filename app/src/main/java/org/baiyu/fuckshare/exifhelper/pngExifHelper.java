package org.baiyu.fuckshare.exifhelper;

import android.util.Log;

import org.baiyu.fuckshare.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class pngExifHelper implements ExifHelper {

    private static final Set<String> pngCriticalChunks = Set.of(
            "acTL", //   animation control
            "bKGD", //   background color
            "cHRM", //   Primary Chromaticities
            "gAMA", //   Gamma
            "gIFg", //   GIFGraphicControlExtension
            "gIFt", //   GIFPlainTextExtension
            "gIFx", //   GIFApplicationExtension
            "fcTL", // * frame control
            "fdAT", // * frame data
            "hIST", //   PaletteHistogram
            "IDAT", // * image data
            "IEND", // * trailer
            "IHDR", // * header
            "pCAL", //   Pixel Calibration
            "pHYs", //   PhysicalPixel
            "PLTE", // * palette
            "sBIT", //   SignificantBits
            "sCAL", //   SubjectScale
            "sRGB", //   SRGBRendering
            "sTER", //   StereoImage
            "tRNS", //   Transparency
            "vpAg"  //   VirtualPage
    );

    @Override
    public void removeMetadata(InputStream inputStream, OutputStream outputStream) {
        try {

            BufferedInputStream bis;
            BufferedOutputStream bos;

            if (inputStream instanceof BufferedInputStream) {
                bis = (BufferedInputStream) inputStream;
            } else {
                bis = new BufferedInputStream(inputStream);
            }

            if (outputStream instanceof BufferedOutputStream) {
                bos = (BufferedOutputStream) outputStream;
            } else {
                bos = new BufferedOutputStream(outputStream);
            }

            byte[] byteArray = new byte[8];
            Utils.inputStreamRead(bis, byteArray);
            bos.write(byteArray);

            byte[] chunkLengthBytes = new byte[4];
            byte[] chunkNameBytes = new byte[4];
            long chunkLength;

            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkLengthBytes);
                chunkLength = Utils.bigEndianBytesToLong(chunkLengthBytes);

                Utils.inputStreamRead(bis, chunkNameBytes);
                String chunkName = new String(chunkNameBytes);

                if (pngCriticalChunks.contains(chunkName)) {
                    bos.write(chunkLengthBytes);
                    bos.write(chunkNameBytes);
                    Utils.copy(bis, bos, chunkLength + 4);
                } else {
                    // skip chunkData and chunkCrc
                    Utils.inputStreamSkip(bis, chunkLength + 4);
                    Log.d("fuckshare", "Discord chunk: " + chunkName + " size: " + chunkLength + 8);
                }
                if (chunkName.equals("IEND")) {
                    break;
                }
            }
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
