/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maps.tiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import eu.geopaparazzi.library.util.FileUtilities;

/**
 * A MapGenerator that downloads tiles from the Mapnik server at OpenStreetMap.
 */
public class CustomTileDownloader extends TileDownloader {

    // http://88.53.214.52/sitr/rest/services/CACHED/ortofoto_ata20072008_webmercatore/MapServer/tile/{z}/{y}/{x}

    private static String HOST_NAME;
    private static String PROTOCOL = "http"; //$NON-NLS-1$
    private static byte ZOOM_MIN = 0;
    private static byte ZOOM_MAX = 18;

    private GeoPoint centerPoint = new GeoPoint(0, 0);

    private String tilePart;

    public CustomTileDownloader( List<String> fileLines ) {
        super();

        for( String line : fileLines ) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }

            int split = line.indexOf('=');
            if (split != -1) {
                String value = line.substring(split + 1).trim();
                if (line.startsWith("url")) {

                    int indexOfZ = value.indexOf("ZZZ"); //$NON-NLS-1$
                    HOST_NAME = value.substring(0, indexOfZ);
                    tilePart = value.substring(indexOfZ);
                    HOST_NAME = HOST_NAME.substring(7);
                    if (!value.startsWith("http")) {
                        PROTOCOL = "file:";
                    }
                }
                if (line.startsWith("minzoom")) {
                    try {
                        ZOOM_MIN = Byte.valueOf(value);
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith("maxzoom")) {
                    try {
                        ZOOM_MAX = Byte.valueOf(value);
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith("center")) {
                    try {
                        String[] coord = value.split("\\s+"); //$NON-NLS-1$
                        double x = Double.parseDouble(coord[0]);
                        double y = Double.parseDouble(coord[1]);
                        centerPoint = new GeoPoint(x, y);
                    } catch (NumberFormatException e) {
                        // use default
                    }
                }
            }
        }

    }
    public String getHostName() {
        return HOST_NAME;
    }

    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public GeoPoint getStartPoint() {
        return centerPoint;
    }

    @Override
    public Byte getStartZoomLevel() {
        return ZOOM_MIN;
    }

    public String getTilePath( Tile tile ) {
        String tmpTilePart = tilePart.replaceFirst("ZZZ", String.valueOf(tile.zoomLevel)); //$NON-NLS-1$
        tmpTilePart = tmpTilePart.replaceFirst("XXX", String.valueOf(tile.tileX)); //$NON-NLS-1$
        tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tile.tileY)); //$NON-NLS-1$

        return tmpTilePart;
    }

    @Override
    public boolean executeJob( MapGeneratorJob mapGeneratorJob, Bitmap bitmap ) {
        try {
            Tile tile = mapGeneratorJob.tile;

            StringBuilder sb = new StringBuilder();
            sb.append("http://"); //$NON-NLS-1$
            sb.append(HOST_NAME);
            sb.append(getTilePath(tile));

            URL url = new URL(sb.toString());
            InputStream inputStream = url.openStream();
            Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // check if the input stream could be decoded into a bitmap
            if (decodedBitmap == null) {
                return false;
            }

            // copy all pixels from the decoded bitmap to the color array
            decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
            decodedBitmap.recycle();

            // copy all pixels from the color array to the tile bitmap
            bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte getZoomLevelMax() {
        return ZOOM_MAX;
    }

    public static CustomTileDownloader file2TileDownloader( File file ) throws IOException {
        List<String> fileLines = FileUtilities.readfileToList(file);
        return new CustomTileDownloader(fileLines);
    }

}