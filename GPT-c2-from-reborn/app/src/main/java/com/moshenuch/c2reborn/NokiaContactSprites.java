package com.moshenuch.c2reborn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

/**
 * The exact phone/bust PNG pixels from HTML v6.343 listIcons.CONTphone and
 * listIcons.CONTbust, originally embedded in index.html.
 * Reference upstream blob: 2aabdff3423d1fdf76d1e1c5b7608348f916ce60
 * Cached once and decoded locally: no network calls, WebView or replacement art.
 */
public final class NokiaContactSprites {
    private NokiaContactSprites() {}

    private static final String BUST_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAFQAAABUCAYAAAFrbCBXAAAJI0lEQVR42u1cf2hbRRwvBPyjUBAG3QwUBtVCkWrggTCcrLC56aAwmQ4HE7YyyzbH/ikI04kBpQxpEa2li3Wro7WbWrUa" +
            "h1Taxvoj+8PU1gnq2+baNE3aNOnaprVtsh/n+8Y7Od/eS+6Se3kvTR58yPp2+ebzvnfv7nvfH1eCECphxf//wNedO3dRInErRv7Wbaw0QgRpG6+sxBFB2sbKJS0traCJiUB6GvPzy2jr" +
            "1idq4TNl42h0Eamh23hmZh6p8c47bQ2ajYPBOaRGd/fHbj3OLr9/FhE888yzbnhgzcZcPcjUkO5mgK7K8FWq7ma9hptJ9wYCwViqhiXQtQB46pQNoUvd7m+cyucXeg1tWt16T0Ot7gTc" +
            "01CrKwFaPy21tLzro7sSujbznhEJ7ZvUdevWbaTG2bPnGug2vEKr1tYSSAvZCC1xuTp8y8triIZye39WQmHgKGhqb3f5Hnnk0R4yFDMS+sknn2+/eXMJpQL+QTah337raZ6dXUAsoKeO" +
            "lEKnp28iVnz6aV8vk9DBwR9DgUAEsUBh6uTpqHLcKQAYl04Fjbjnyf37mHVqyBslXKj6unr12gEyU+PZ2sY9TlVXuda7n5XQsbFfXfQyQS0XUsZCv/zS7daZTKRsHv8gbYpQJklWQiX1" +
            "DIVnqayElu3Y8WQvWSzxgtmYbe+XYFau7u4LV+BTgSMrocrqfBBW6tHR35LzKPybNsa4haabSwOBGZlLaDA4G9IyEzSsQYlZKOsEjSdpJqGSnp2iBXiVmYTyzPxMywk00jOUtMAslHUp" +
            "wcsJk1A7Xjr+Z3TTGBj4IYRfhJ28g5+sQ3XUGgWftfh+DZcxYck1yjSirNfdu3c7FSCMUQUO1u8aTrS9/cye27fvoFSIxxOwYSo1jeiRI0frEonbiBVg65tBdHM4HPHF47cQK7TM+lwQ" +
            "rTp/vqt1dTWBWGEW0TIFW/7+ew2xYNeup3rMIkom28bl5VWUCkePHuvHbTeY9taTXcvJk696YrEVRGN8POnUcuE2dlOnJ+LkoZYxF0YDvucwbR71en/ekM4W1MP16/7eXBAtn5uLIREY" +
            "Gfm1h97diSTqiEQWkUj88cdfHjJ2RRG1h0JzsXB4AYmGnlWfKVEuI50HfX2XWmHxEEZU0SgyAuDb1jK/MyY6NRVFRqCrSzBRBc2HDr3YPzkZQSIwMPAj2XvAlsEh8q3fQO1PGqmJnRcw" +
            "JvdSsnK2MpXhKaYKQ6JA7tlxO2NXprzZMxWJFiRRjq1yrYJxarv8dgnHlROiEHymfd40zp3rrLMCURvs2dPt60+cOPG0qUQHBgadWs5+nQBApVlE7TzOh23bttWaRVTSinLoAaIiqTZ4" +
            "hhLl8ZJgopIpRPVixloYHR0zjyiPO+fUqdecphGdm5uPa8W4dOJee00jeuzYS/2svidMcrMZRKshaqgV4FPjlVdOeejAdK6Jwo9KZ86870vnJMM7AsnMJRR+3ElHN9XYtOmBTtyuwkyi" +
            "ZSRiqvbkAbZv39GLHWeSFawnCDhtAbJHjhzrHxwcHj99+i0v3sjtTzc2c2rmYdciEDqJCTZh8lK6aEhOiCoxbA+JZSvR4VhbmyvpE3U639g/MRH0kf8DhEKRvTknOjs7787UP9rWdsae" +
            "E6KRyEIsU5IEO3fuesxQorJ8wyPKkYsXDEOI1rBkMbDC7w/JtH0qiqgNBIv2OAv1PRFt8qRwsKKv72snmb5EEZWMcIsHAuEQyVAQQhTSo41yjQvz4YNfdHj4sisfiNq/+87r4kne4U30" +
            "gZdVBNEqj8frMirYgImWCSL6k4snc4k3ywl6TQjRoaGfXEZFRYQShUgxT9oWKy5c+EwWSbScGMZjY3/GRIVvcO1Gq8i3nuyNakkIBuJNkKOml7+mBVn2x6E6gQrjNGG51aKNEmLFQ/Jc" +
            "cxZxpkYqvFNhlD1qwxqgY0m12AvSgEk4qUS+BmpzJ2kRzNWeqRSbapUawbBqfK8CR/1sxYBYkeg6RlEJRYUWgEJFXaHQdKUSPPVQUd+UiMfjbiXQai8ReK0HhdpGRkZe1otMsyCRSMRa" +
            "Wlr2sbo317NCKxcXY3K6CDorotE5H7ZEbYWmUHhgx8LCoswa6WeFMm2QaFZZISm0BtImeDISePDBB2cbeCIc+a7Q8n/TO0MenoSEDJMXqgpBoVX/+oMjPp7ECb4ki/8UKhWMQqHYmCfB" +
            "gwcDA0OtBafQ+vr6k6urcWQEHnzwoeOFpFA7qc65dOkbeWVlDYnExYsfX8HOu4KZQ23U6GmanJyKsWYhpQPIwvkMUroitPVmNpHjOmohbycSuRlPl4SUDn5/UpmtVFJIdaHtlCrpwryJ" +
            "ianY0tIqygQez/fjVKGJlM1uKd+3nlV0hEWrRDMVwuG5OE76okemgyWvaj07RyqoOS953MK+fc+7b9yYjC0uriAtdHdfvIJT/FyqU32qsyWzXtx3pVR4bQuVKMdaquUQ4WlaFwpVkvTu" +
            "h4NqFhaWO2dmot6hoe+bNcJ69+Dw4YY6vz/oCwbDMnwXH3Zzf8EpVHnoPQrG6axHI6Bkpk1evvxzHe/ilBcKVSrat0xPR3/PNiEyU0xNzcgHDrxQnfcKfe65fTVKzm1IVNJmtggEpmWc" +
            "pWrLN4WWKStxYzQaQ1bEe++1N+hZBFZUqF0p4neKTn4Vja6uC41atQBWU2h5ff3hOqsrk+Dxx7fWllAnfllNockzXq5d8/uMyHo2AmNjv7vVZV9WUmjVoUOH68LheZRPoGzbUispNFlk" +
            "1d8/1GxUYrZROH265Tg9Sq2i0KTTeHjYuARyowCcae++VRSa9BpBRr5Rie5Gn9bDmkifU4VC5YBRCflGQaXQcksp1MjCAaOPaWLN98+pQjs6zjfnm0J3765rtaxCwdm7ceOmzq++6h83" +
            "onJEJDo6PiSHZB+34hx6H0VqJ3YA/1c00tPzmSyq0iUTyPJkHDjgKhk9J7XdilvPzSpH8P5UxS0PP1zTs2fPs+7XX3/TC1U3H33UK0PFDoHX+0tUXaED9+g28B34LsgAWSAzhae/WRUy" +
            "IWETm9W9TTasXIeGtx1G8EGqCMclGE1Y9kEq4UHSOFiuNN899jaceFChUe1kBBw4VF3OE28qFi0Uq0CKCi2CA/8AaokazqulO3gAAAAASUVORK5CYII=";

    private static final String PHONE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAADgAAAA4CAYAAACohjseAAAG90lEQVRo3u2aaVATZxyH06Yeg0aOIAEkAoJRxCiCoiiWeAFaU6R4AlJQDhGqYjQkDcSAigdeCOoErakgihqPiqXFUm0d" +
            "ixfttNbaBurgUagDeKX2A59+fZfZpKQqomASOtmZZ5LdfTf7Pvt/7w0DAOP/DMMiaBG0CFoETSJo7C375FPB2pN/K3RQ+wSbjlxrzoLMVXs1CxTHHt/NPvEUFJLDf0H3nSKztKlcXtq0" +
            "mErbrQRTd1wNyDj455Xs41p8pHqM0A0PMELcqCcgswkR2x5iZdETUGkURx/dJdcInydqboI9V++r3ZKlfoL0kscIyWkEf9X9dpmc3Yi0okdovUZVp46WqR3MUjBKeoyfUXL/JimSiN7Z" +
            "BF9xA4aLOk54biOoa2XF9RrBHImbOQkyl+VVr5AfbtLKDj3EpDUkw2n3XosJsnpQvyFR3a4mv8s2B0Er8b5ateLIA6QUNsJHdBfey+90iiDRNSRt/Z7KpA/18EwmKJgtdpMV1d9cU9qM" +
            "Obn1GLasrlN4pdTCfd5n4Ar3of84kYbcwo/AMolgVPrhKbKiPxo+Lm7EVPkdeKXe6hS8+Gq4hhXDJbSgxZonPE9ukUMLWhlbkBm3piwi42CDVrT/T4xbfQtDl9bqGbLkJjyiz2FQ5Fl4" +
            "xlxo3W97/nlQaV1mKOE0Kae5d39vNbmHlBBA4Bm7DjIT1leK5Qfv4yNlPfipGgxd8i+DF1XD5b29cJm+B46C9c3UJ1eoaj3eNp2OIYk34Bpe2pre3n+5htnLWkXuEUtHjmvsVpSZsO6s" +
            "OLO4AUkFdzF86a8kgzf1eCy8QDKqhGPQ2uYe/biHSHplb/thamp/QMguDIo6Z5DeM6aqNb3ztB0tfV0FFST9FkIw3bDYGLsfZCaspeTqEZ1bB17CDQMGLajEgJACOL6b3dwmClRmE6l9" +
            "6jh13mPhxdb0brNP6dPTD0NEF0kvaqBg7JGM1YcZJzMyDvyBqM23MHjxdQPc558lUciHQ8DHDURG2SYKzvRnMHWcE5jV7Byyh9Q1VWt6u5Hx18m5fMICukg6m2IsylwkL5NmfHoPURtr" +
            "4Rn3owFuc7+E09Q82Pku1dCZDaajYKW7nt4PJpFS9Q+QNnCC1mv7cIMq6FZS9zBYpphNMOOzysUy1R1EbqiBx4ffG+A6uxyOk7fDblTyf+X+O1im9vmECKpe0qS2aSWZppguMRdKjiTL" +
            "9t9GZM5v8Ii5ZoBrxBkitw12Pks0bSLh1U5mrehICWjaLZJvXHDmolyhZO/v2uS8GgyJvYpB0Vf0DAw/TbqALbAdmaSTC3iJnH6WQXCgxXq+SiPQpYLeY8P4ksIabfIODXgxl+EeeUkP" +
            "d9YpcIJyiVziq8p1autKQXZa3g/Voj01GBV/GW7zL+pxef8EHCZugg0/wahyXSlolaAoV0oLazAh5Spc517Qww3/Ag6BG8Aek95MNygCY8l1lSBzVmJerESpQbjsBwyc840ebngFidxm" +
            "sEeLm9/u2U/ZgQbF/ARHjI/wWpX/U0Ni7s/gRpwzwHFKIdhj17SYSq4rBFmL5WXK9D2/gh/zLYlYpQEOQXmwHhZL1TuhKeQ6Leg/NS44fdcvCBNfgUtYxTNwBLtJ3ZM36OZmDBNsnRFk" +
            "J6/7umzFjutw+6ACA4RfPINTyBGw/RWwGbns0MuGVGYn6O41PkBc8DNCV1yE88zPX4iDoBB2Y+ToNzQuozsVUfb8ZZ8oVu/8CYMjyuE043S72AfmwXZ0Jonkyn3GLqqvK+gRJz2uXLzu" +
            "KhxDT3UI+8CdsPWVoZ/3kt3GlHxdQa84iVq5UF4Fx+ATHYIz7RiJogLW/OXn6RbVrAX9YtOPKRdlXQJnqrpD2PlvJEVUgh42Q/LpVtWsBfnzU/fmi7ZVw2HK0ZfCHp8P65Hp6O08pYqe" +
            "25l9BHn8cbOkoq3X4LvgNPpPKn0h9hP3k2K5Gn09o+vo9RM/Y3YZrytILcsJk7MqtGHLK2EfVPJ83i2C9QgpWF5LtW/3YG2hZxHO3aKboCIxPTKrKi33Mpwml4A9sfgZrEdlgeW9Eu/0" +
            "dT9Ej0V53aUfpDpsPxcPX0Xa5ksITSoHO/CAAbZjtoM1bAV6OQZV0UuBPt1tLEq9gxMGz5FVp236Dh6hJbAbv78VW/9dpFimwcp1Xh29lG7UetdVglRn7cey4eQkZZ7RxkgrwZmogs1Y" +
            "JYmcGH0847VvvdM3n653XIaJts5Ol6goClwH+6tTs75qiUn/Chz/dejDSwGzz0B1mzkgo7sKMum6FTvUZ1pZiuJsy7TIAvSwG3O+Tb3r2Z0FGfTLDj+6j9MtzurqnQ3DxFtXLTqxaSEh" +
            "/b7ApPXuTS0bsug6ydP9AaDbClr+jGcRtAhaBC2CFkEw/gH8MdOhA8ZotAAAAABJRU5ErkJggg==";

    private static Bitmap bust, phone;

    private static Bitmap decode(String text) {
        byte[] data = Base64.decode(text, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public static synchronized Bitmap bust() {
        if (bust == null) bust = decode(BUST_BASE64);
        return bust;
    }

    public static synchronized Bitmap phone() {
        if (phone == null) phone = decode(PHONE_BASE64);
        return phone;
    }
}
