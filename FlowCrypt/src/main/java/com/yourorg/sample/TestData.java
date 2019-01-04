package com.yourorg.sample;

import com.yourorg.sample.node.results.PgpKeyInfo;

import java.util.Arrays;

public class TestData {

  public static final String ECC_PUB_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
      "Version: FlowCrypt 6.3.5 Gmail Encryption\n" +
      "Comment: Seamlessly send and receive encrypted email\n" +
      "\n" +
      "xjMEXAZt6RYJKwYBBAHaRw8BAQdAHk2PLEMfkVLjxI6Vdg+dnJ5ElKcAX78x\n" +
      "P+GVCYDZyfLNEXVzciA8dXNyQHVzci5jb20+wncEEBYKACkFAlwGbekGCwkH\n" +
      "CAMCCRAGNjWz4z6xTAQVCAoCAxYCAQIZAQIbAwIeAQAA5H0A/3J+MZijs58O\n" +
      "o18O5vY33swAREm78aQLAUi9JWMkxdYOAQD2Cl58wQDDoyx2fgmS9NQOSON+\n" +
      "TCaGfIaPldt923KqD844BFwGbekSCisGAQQBl1UBBQEBB0BqkLKrGBakm/MV\n" +
      "NicvptKH4c7UdikdbpHPlfg2srb/dQMBCAfCYQQYFggAEwUCXAZt6QkQBjY1\n" +
      "s+M+sUwCGwwAAJQrAP4xAV2NYRnB8CcllBYvHeOkXE3K4qNQRHmFF+mEhcZ6\n" +
      "pQD/TCpMKlsFZCVzCaXyOohESrVD+UM7f/1A9QsqKh7Zmgw=\n" +
      "=WZgv\n" +
      "-----END PGP PUBLIC KEY BLOCK-----\n";

  public static final String ECC_PRV_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
      "Version: FlowCrypt 6.3.5 Gmail Encryption\n" +
      "Comment: Seamlessly send and receive encrypted email\n" +
      "\n" +
      "xYYEXAZt6RYJKwYBBAHaRw8BAQdAHk2PLEMfkVLjxI6Vdg+dnJ5ElKcAX78x\n" +
      "P+GVCYDZyfL+CQMI1riV1EDicFNg4/f/0U/ZJZ9udC0F7GvtFKagL3EIqz6f\n" +
      "m+bm2E5qdDdyM2Z/7U2YOOVPc/HBxTg9SHrCTAYmfLtXEwU21uRzKIW9Y6N0\n" +
      "Ls0RdXNyIDx1c3JAdXNyLmNvbT7CdwQQFgoAKQUCXAZt6QYLCQcIAwIJEAY2\n" +
      "NbPjPrFMBBUICgIDFgIBAhkBAhsDAh4BAADkfQD/cn4xmKOznw6jXw7m9jfe\n" +
      "zABESbvxpAsBSL0lYyTF1g4BAPYKXnzBAMOjLHZ+CZL01A5I435MJoZ8ho+V\n" +
      "233bcqoPx4sEXAZt6RIKKwYBBAGXVQEFAQEHQGqQsqsYFqSb8xU2Jy+m0ofh\n" +
      "ztR2KR1ukc+V+Daytv91AwEIB/4JAwhPqxwBR+9JFWD07K5gQ/ahdz6fd7jf\n" +
      "piGAGZfJc3qN/W9MTqZcsl0qIiM4IaMeAuqlqm5xVHSHA3r7SnyfGtzDURM+\n" +
      "c9pzQRYLwp33TgHXwmEEGBYIABMFAlwGbekJEAY2NbPjPrFMAhsMAACUKwD+\n" +
      "MQFdjWEZwfAnJZQWLx3jpFxNyuKjUER5hRfphIXGeqUA/0wqTCpbBWQlcwml\n" +
      "8jqIREq1Q/lDO3/9QPULKioe2ZoM\n" +
      "=8qZ6\n" +
      "-----END PGP PRIVATE KEY BLOCK-----";

  public static final String RSA_2048_PRV_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
      "Version: FlowCrypt [BUILD_REPLACEABLE_VERSION] Gmail Encryption\n" +
      "Comment: Seamlessly send and receive encrypted email\n" +
      "\n" +
      "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\n" +
      "=15Xc\n" +
      "-----END PGP PRIVATE KEY BLOCK-----";

  public static final String RSA_2048_PUB_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
      "Version: FlowCrypt 6.3.5 Gmail Encryption\n" +
      "Comment: Seamlessly send and receive encrypted email\n" +
      "\n" +
      "xsBNBFwBWOEBB/9uIqBYIPDQbBqHMvGXhgnm+b2i5rNLXrrGoalrp7wYQ654\n" +
      "Zln/+ffxzttRLRiwRQAOG0z78aMDXAHRfI9d3GaRKTkhTqVY+C02E8NxgB3+\n" +
      "mbSsF0Ui+oh1//LT1ic6ZnISCA7Q2h2U/DSAPNxDZUMu9kjh9TjkKlR81fiA\n" +
      "lxuD05ivRxCnmZnzqZtHoUvvCqsENgRjO9a5oWpMwtdItjdRFF7UFKYpfeA+\n" +
      "ct0uUNMRVdPK7MXBEr2FdWiKN1K21dQ1pWiAwj/5cTA8hu5Jue2RcF8FcPfs\n" +
      "niRihQkNqtLDsfY5no1B3xeSnyO2SES1bAHw8ObXZn/C/6jxFztkn4NbABEB\n" +
      "AAHNEFRlc3QgPHRAZXN0LmNvbT7CwHUEEAEIACkFAlwBWOEGCwkHCAMCCRA6\n" +
      "MPTMCpqPEAQVCAoCAxYCAQIZAQIbAwIeAQAA1pMH/R9oEVHaTdEzs/jbsfJk\n" +
      "6xm2oQ/G7KewtSqawAC6nou0+GKvgICxvkNK+BivMLylut+MJqh2gHuExdzx\n" +
      "HFNtKH69BzlK7hDBjyyrLuHIxc4YZaxHGe5ny3wF4QkEgfI+C5chH7Bi+jV6\n" +
      "94L40zEeFO2OhIif8Ti9bRb2Pk6UV5MrsdM0K6J0gTQeTaRecQSg07vO3E8/\n" +
      "GwfP2Dnq4yHICF/eaop+9QWj8UstEE6nEs7SSTrjIAxwAeZzpkjkXPXTLjz6\n" +
      "EcS/9EU7B+5v1qwXk1YeW1qerKJn6Qd6hqJ5gkVzq3sy3eODyrEwpNQoAR4J\n" +
      "8e3VQkKOn9oiAlFTglFeBhfOwE0EXAFY4QEH/2dyWbH3y9+hKk9RxwFzO+5n\n" +
      "GaqT6Njoh368GEEWgSG11NKlrD8k2y1/R1Nc3xEIWMHSUe1rnWWVONKhupwX\n" +
      "ABTnj8coM5beoxVu9p1oYgum4IwLF0yAtaWll1hjsECm/U33Ok36JDa0iu+d\n" +
      "RDfXbEo5cX9bzc1QnWdM5tBg2mxRkssbY3eTPXUe4FLcT0WAQ5hjLW0tPneG\n" +
      "zlu2q9DkmngjDlwGgGhMCa/508wMpgGugE/C4V41EiiTAtOtVzGtdqPGVdoZ\n" +
      "eaYZLc9nTQderaDu8oipaWIwsshYWX4uVVvo7xsx5c5PWXRdI70aUs5IwMRz\n" +
      "uljbq+SYCNta/uJRYc0AEQEAAcLAXwQYAQgAEwUCXAFY4QkQOjD0zAqajxAC\n" +
      "GwwAAI03B/9aWF8l1v66Qaw4O8P3VyQn0/PkVWJYVt5KjMW4nexAfM4BlUw6\n" +
      "97rP5IvfYXNh47Cm8VKqxgcXodzJrouzgwiPFxXmJe5Ug24FOpmeSeIl83Uf\n" +
      "CzaiIm+B6K5cf2NuHTrr4pElDaQ7RQGH2m2cMcimv4oWU9a0tRjt1e7XQAfQ\n" +
      "SWoCalUbLBeYORgVAF97MUNqeth6FMT5STjq+AGgnNZ2vdsUnASS/HbQQUUO\n" +
      "aVGVjo29lB6fS+UHT2gV/E/WQInjok5UrUMaFHwpO0VNP057DNyqhZwxaAs5\n" +
      "BsSgJlOC5hrT+PKlfr9ic75fqnJqmLircB+hVnfhGR9OzH3RCIky\n" +
      "=VKq5\n" +
      "-----END PGP PUBLIC KEY BLOCK-----\n";

  public static final String RSA_4096_PRV_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
      "Version: FlowCrypt  Email Encryption - flowcrypt.com\n" +
      "Comment: Seamlessly send, receive and search encrypted email\n" +
      "\n" +
      "xcaGBFwFqkQBEADxLDVykJKqNCBGHqF8Hw2lLkCWnR8OPGmoqMALl+KstBPm\n" +
      "7vraDYy/JDRZ6Cju5X7z8IrIrrM7knyjz3Z/ICYjdpaA5XSCqMjrmlXbhnRH\n" +
      "rdy/c5/ubQsAgUB9VqjNEpYC1OZ9Fz8tB0IiHgq+keIVh/xKf7EAvq1VYLZO\n" +
      "k8kE81lvNeqX0hXo2JVvGiQ6fuBv5w4shvDzKfirsIepxaLwj3GJUcW+zhrg\n" +
      "QztuoRskr+PerGp4sf5sX8pci/kDuwaYFXJ4DNqCt/LLZ+XtxhyHDW4Dbh5f\n" +
      "LKXWoNq7RPkCX18aA9nRCPwuyxKd6TkjzwKSm0r16ResgnnCVGeqjBHxlyQq\n" +
      "RDR9MhmjOvmEuZ19axnwcwBbFHvmcSy8Or/RMuPv4ZusaOEyeC3VLn3Tj+be\n" +
      "BgkikcpMWEJH8nDppEX5hIW2hjsHz3atD21LoXyQFi8c0E6wArcIyDbxWKZj\n" +
      "1/nZkP1Fk3MDk7L/f2YO5LkUDHlhb12zNDJ4B/nggpAODMxqCPF2aoY0ryvg\n" +
      "bru54WG3z2+Z0n6KP3m9mIHQZosBdYCnvKilKotO2SgUqa7B7pPDV7XPynO5\n" +
      "Cprl2CHixIzZ9R50jGkR7q8H4BGWBXXfm8kap0/Yy/rICs6nYAhSAPN6CNny\n" +
      "FpirPawL7iRzkMalvMhrCotJRGiB+qOPPhhFkQARAQAB/gkDCGfXhgmvVIIh\n" +
      "YCzHEZSujH8lhiL+4rbr+u2Z7ZhLq1K545Xv5FNPB3GWX1OMwlurkyw8mVvO\n" +
      "gTMzzcr85tP4yaaknlt7CbvciDo6qBTYqdF4SsNJnZ46zbecb4dcPUU/Xbua\n" +
      "RhAQvVwkpX+uBVEKsSme353NCHAmfAD/iZtqIoh8A4LEgpIArPuyXlotT3LW\n" +
      "093NEa/1N9WjP/OtFfEn5P0afCGXMK8ZOvAb8559WT5XyAUewesC37gwfaXO\n" +
      "rAedOrTkxtAZn6bh6GXZ2SbXxvR/G27L8/sizWMJMIZ7V/kVDk5s6COqxVRd\n" +
      "1kK3JZ2xcZO/kE+oH6RFtKKEATy7fm+HIy9g9z4/Gc9TeOu1WzwnRkXVXMMz\n" +
      "Wg2ks6SnhEB8vnzaeQxN74o4Y/qV56OFHy1jaKed/jaLMIdSRCxYm2o59Jj6\n" +
      "HvBMQg9yR4Ibub/7E76u1X2BqYgkRVn8Z5TdXpwrbrNFleRNHgzu9pk98r+l\n" +
      "4NqwLK0jXQ9LU9NWIktrrNl5FbvwiREVcFJP5dPgXXXh4gjLxbEqaDp/xg7x\n" +
      "YnfjuEC/lonnKl3ej8IdzyiizcYCu2Ic1/oVVMiLscp5/+uL8Q/BdLic6+j4\n" +
      "Cx+UljHTR3Bci9iI0v+hCVub6Bcz/GyXHDoLzMhjN4VK8UVBjf155UuB9a/m\n" +
      "hJ8XzAXld6ObUGOqV9YtiHrhhPChJCgh4M2nLHW12oCuS5Eu5y3aQO4jLA/D\n" +
      "SlLHZe8Gzmv0zAv9jldoIz5l86Yoao2BaGmyL2QIlQUoE8+fOwVCcf61nLjN\n" +
      "gVhdiL/8JybxNZ18dejJVFUaYP8VdcT7bpg04X5nLe2GmSG4T3DFXtF6NIgT\n" +
      "jSdHnheqDSjB1pQXkS/VjRXGZVyHSMP9RVrNMVdy2KhMcEWw/Ci97ORlt66N\n" +
      "iSI+D8a+l6TNajX6XkZg+Mm7tX6Aa6ecdgkMndogFqISZC+Mcumzn8ftBL9l\n" +
      "0sW/dnio3JK9Bv5rNo5AB4MUGJTun2Cy14yPkEzfYpyC0KiYWfnK/Hjplp36\n" +
      "wwJ/944Q7VRJc4RZfjC0nb5sgfnh4ynYSyxauMhziZlai+FOCkuYOLWNHx1d\n" +
      "S5TTm9AthQsTPBH7o7r41/ujV53XSgpEaFUFTB8KUd9FREbEUSxT7j6RmO0r\n" +
      "jilWBepNPjPnQBgu9PnfQl2TsUor7r6pMBrpQidRSr5bWRVCi7zj/+CPaXaY\n" +
      "r99DIOhEGVIXhlNSOBO1bCHKgMt4lRsKTF0sWcyf7P1wVriSl5prU1ffpk9t\n" +
      "yoNGIIEpEw6J8B6VHBoi6WQr/zvqSYAmLwMZgoK7p4HDV87yQvbUhdiAxlT4\n" +
      "w0zLy0bYUJ5trfUYeLt40eppMed9iJj+BaXyxxXWiIcE12v9TkmIHGwzgzst\n" +
      "RGaSU4Q3utGLuqEhh8HvKlrhSv7iQtAdbJ299iVk2fLUPG73OhzI1ESHYBsb\n" +
      "VTYnWZTvuFy+m/Odxma5FOI8e/Zd85+FNwpPHBrbImexLqDxXeArmCoIItiX\n" +
      "bleAWDh+Qx0m7akPcPSYtXWAlQjm/TdGGpaNBvfcEh6GNZOqEHuEIpspxlNM\n" +
      "FN0HDP9WKZY06WYdIuEt9slJEhifICpt6X5ZD0MyA8904C6pf+Dt4w4DNZQW\n" +
      "aChVC6XADH5/mBOBssQF1rqfgC/JvWsci9oWo551uJqgDg8WqqXZr9WRLZ9n\n" +
      "rSPFtx40TrTyuXcJDpi9A84/6usTXcye7NBbdIq7h0enygBtnw20i6G7a9YR\n" +
      "XMx/Y4jSatIoL8urrjTs9QAvzO3NEXVzciA8dXNyQHVzci5jb20+wsF1BBAB\n" +
      "CAApBQJcBapGBgsJBwgDAgkQfDB+byCSli0EFQgKAgMWAgECGQECGwMCHgEA\n" +
      "AMFnD/9S9stZYoeWjGfcahaVY+ke+412exJWpjT6JDLMdrZBingzFhduJ51c\n" +
      "SqWnnvKnj2d3imDAIsiAaEenaEenIr9JJ5XlYrqBTLJtrU3MR1cElAcTBOfz\n" +
      "ycUnD2AY05dNkmSENiu0IkMciazVaVChrJjVfH82uv2mb9NKZhofcNqV2S0c\n" +
      "8LGrsKgpqgeudmIjWqYLbYn3b+mwNOBuTYR/JZTqrIhf+s696IhFV0GmJd+U\n" +
      "Z25/3z3ohmqyz8HB0TIfSsIbp3G7EhzLcbrbNaZh/lfjIzPepD19Y6k5EEYI\n" +
      "hi8CM2I9xOadRo2lrWhXP+MYvWLvZtJifgM9M+IDdj2ITVCXgKdkpBD+E2o+\n" +
      "2dIE5dK/eMoJy2z5iwxygrbLltmEW0b3jddsoavJXwzcnulgfZP2Dq9jjg0a\n" +
      "1VEx18OIwU1JcjmCfMLNDQmMo8R+mesZ8It6la+F5OnO8Dw8lKQcczxdXPGP\n" +
      "vz4popFFaDpeLTOk0rT6wlE1/oZ2QNP1Ovh19zCp513Ey31JAeU2yxPiEsHq\n" +
      "J/SkQjc8ps9J75S4LC3wS6HUGx+4VFt3cr2ok3muzGreesugjBsQqxdua/WT\n" +
      "NfA2cV6i0UDgJ0PN/rBni37Mo8xhD9ONN34Ad6msTyBszMEHVMLCPWgvZ86I\n" +
      "zCYisg1TYhNMqx8reZf9Iquy5SZdzsfGhgRcBapEARAAr9225iiC41zRrHDY\n" +
      "cqXIGwz2HVRYtnVJWJiuyC5hlSYbrejnVxCfl7/b6F4LQhx4xVWCxo82/9oc\n" +
      "HvyFaewoNRH2xdxMWdx3IWJfHs1pqGSBcTV0L/nA7riTmAG/84hY1p7TaM2C\n" +
      "KO2NzzpSQapUY5iFG6IG1o196BSzRSHDD/o3jOhgKYfg8V1FdCpPZ6z/y5Zb\n" +
      "Wj1CRux054/3lnBsZHUXJ+S0VjSdRimJBamjSjWQSoS7H99P0bJ9TTS+oTyD\n" +
      "Sdocs0t7M/Q6+8NsTo8xWZKaHdxZcRB2Uv373OMoXqN7AA2oQmATYAvuQJhB\n" +
      "XDtAzXCbC1YkMy2SGp4fup70El3F9EvGgKLUHBgtvSqMpirrETxT99nlmLsE\n" +
      "kwY4xPlwAhWMJS7POJzvYYtWeky35TvpSfUXiwEf6MN+kdUzBDTn3Zdb7p/N\n" +
      "VLp7uZgP+netg4oP1cMZV0IOtb1uzbXhPR/9ZbOqifur7j0G2b1eD8+Pfn84\n" +
      "gMwm0nybn3OpAi2RXVdZpPrJlOt7bKCUxmfMimuP4o/PhOc2aUYo+DxdkCyS\n" +
      "LptiSed9/xi9I0ppyukk9jt8MXLSkhQgENFcioUDsQJG8wiOkm1C27lWMjwz\n" +
      "jzRp+EWHcxjcjQgwinJif3gps8U2Bg+Mkuws9WxkQjR4UjosmuXgI23eJpW8\n" +
      "RFn1G6MAEQEAAf4JAwj5olwqnpz1AGBabb4N9PPYszUi42U0dYPP22yfNfWh\n" +
      "R9hBSz+jvIujHsJJyksOSQMCFYVZ0QX5MTjBkjtjs1PV7FsgOJSRILY51WpP\n" +
      "gDvhnUhyHRSrph0l7cgbyezxSfayIntIymfN2BfTBCYHv5y6TIzocCZDXOgI\n" +
      "GhjLfPVUe5Rc0QeOnk13eYiWTOPI+LyQi/mG27BbdZez4nyubH9scsgjY69v\n" +
      "rne42F7e4+Bo8L8Pc5k2ctcabZrmhMbIEH8+EKub7LXqSylS+FnpZCbsICGt\n" +
      "bL/ZOP4X7LMAOmLKVLonqr3h5ihpcsIU8MbME5IZSI6qSGWf4/Sly93+Zw7+\n" +
      "VetmH83yTkVPfM6ah5XU5HiY7H3LZ/4DeRoIqRC4S+Ym4tef5+F2lGLGTSgx\n" +
      "PtqBOwFrpVn3ary0ecfOQQDQKvWwkj3vYURUH5ze5o+zcgMgXe0K+EGVXzm4\n" +
      "JMsYReE4UG3LRdHv2QME0bd/okRwpp4TA2gxC9IaQ76u1ZDTdEUk/zXjgQ/S\n" +
      "B87+wH6N6FUgO9ER8Jj7L1epwECXYSaKV6P+rO5rre1R1NhqQ8keI36Fz/Vy\n" +
      "eXBB+haxSvVKkGcnWdGWJ5vbDBsBhcZfT1fF+NN5l5a5g6qgms6s+0bpvTmd\n" +
      "rPVp03goqRXKgH/gb05X4xzOtBGZrKR732CtpuODXtpfvleuJKroSpm5BbhJ\n" +
      "g6JZyyGn0Rnvj+TSCBarBkLGecRMdyAvXeLZUOtapW5wO0V4JJqi3P8mmr1n\n" +
      "sNVYzjmVCkx6qTrn3M1wMbUznci1oZuSzy0COukF6qTYyGiKe5yn2D0Ue/jj\n" +
      "jsaJjHY5mgX/ZEjgN+qDDxW7ANt+sGWnJZqvRcq47YJSrbGyPcdg0gaYRm7v\n" +
      "gIXvEEhZy5YNmVDxTyL2qPQzsbhJAi62PbUWgnvovbLnwYwSbf1o9MtCTVF+\n" +
      "MdR88iUQW253elP5uF9oMUaZUzgbyr4/RCcRMpb3kumbBTJDRDjE44o6rEnl\n" +
      "JkXK3itvOqfrfM349NVnub46u811tjwws/3yw1nxOPN2xG6vErPNftHihu4H\n" +
      "UL0X5/w3h1qqjIc/cCihprfOwREIT6neV11X4Q68F1rJIOwV6sx5ke8G3ius\n" +
      "kvpncAY0SygzjNwMbE5Up11lNF+MNu3mB6oxIq8q3SIc7ki97enCGnJIknuy\n" +
      "/wkgZPyFoSBuBnyc5hTcM27LTxFHzMuholkHdTdRaJcPTddhvLb4fsxcljxt\n" +
      "OiR51QL+EwtZvIa5RdjFYitLgS0yeW3dJ20f48X4D4MAEjOdqVRj4YLCU9qw\n" +
      "r0DPI7jYab582cmlILTk1X/GnYCp2x1AHHzzXanVc5O90YOa4cOCn4lTFrnh\n" +
      "2bM4eURX9N4sw+QCKz2X/BNZToM7uVcuRHbF0DhFVly8Gfh5jAtEwbq3n79n\n" +
      "SWXrXYD+121hqXCvyGa46GI1wS6fTJR3RlOojIh/e0WktuTzvvC0TQUzdX4H\n" +
      "Ei0PY95JWvH1TdtXYvfzJRWvckcQBTJevPX52w475uwpsyF1hc0U77R6IaGV\n" +
      "+aW9eE9bTlFfUYtiGmvGe60M90r82QASn6k4w5vuEydNUk07Mr6ZSWlNSbD6\n" +
      "p5Th5Oi9NxOb4/gR6JTvekF28CYyqTU2dU8j8/JMIrUi8MIKYdNCpqr5pBFs\n" +
      "aVRZqoG7+mmVvlv/I9NgvzK3mvt007qPLRmaBZNifkZwKk66DDy2WeOqn0yB\n" +
      "JAkiG6/pLuN6IqNoDKUuK0rJx0yCuWenQKqIpDX748uHl9DEAOrl1ucVwsFf\n" +
      "BBgBCAATBQJcBapICRB8MH5vIJKWLQIbDAAAKr4QAK+czw5qcXxB7z7KuHXs\n" +
      "MQcEOInm/FIvT4mDeDJFUeg8/bM6gbYGQ6IzRUcKlLHCTAHrN1oGjA7wauL3\n" +
      "C9xhNRAtZWnkNMzG28GXJCuz6FWBZz4sfHSzQZ406SG1ewORw9G7OuOW5ynF\n" +
      "3CD+S1FLxIhqO4ZyMc1ZnmYouwdnP/ZnAVdRCSgovFws/dZH5kPPuOHYYzME\n" +
      "PYNS0dQOBOXtDPmDUSQfN3byqNFOojdujgX69do7mCFeYivI9p+wHbPljkZi\n" +
      "tlvoBcLtE9gdrnQxN9XiZlVPU1DiFmu/QWf+oeHUhlIjxnt8+Zu8nnCQ0PWf\n" +
      "QXK44JMqJ8OOTxjdc20p84MOvLE4vTXK7Dl7ErtbOywIc66CTDrUbzf9zeNX\n" +
      "qIsKAHX1i0PZzaUxlo9vIW4SrX9QfiuLvSh3LZUbgfgD4LlXmietMweKTN9Q\n" +
      "sZ+4th8IS56W64vGArDIXOd4RqZMX4l92ZshN216ZSN7NbmOslTaQwFHyX21\n" +
      "UM5TKUuMaiWqYQf9pXzsqTda+OOv7bc4u71+kt375jrZ1DwuySuyNRKCvvqw\n" +
      "sq3nqQb7hUO9kXpC2iBY+cVXMyrrnjbppaRr1rH166iHoIICXHQRQRsZqEro\n" +
      "J2d27nYWExZmqJdkCg2RagBXiSkJOu9Hs7F0DEV/wHjOE7Z9mSIyLZiKaO5l\n" +
      "Dn6h\n" +
      "=5aR+\n" +
      "-----END PGP PRIVATE KEY BLOCK-----";

  public static final String RSA_4096_PUB_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
      "Version: FlowCrypt 6.3.5 Gmail Encryption\n" +
      "Comment: Seamlessly send and receive encrypted email\n" +
      "\n" +
      "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\n" +
      "KQk670ezsXQMRX/AeM4Ttn2ZIjItmIpo7mUOfqE=\n" +
      "=eep/\n" +
      "-----END PGP PUBLIC KEY BLOCK-----\n";

  TestData() {
  }

  public static byte[] payload(int mb) {
    byte[] data = new byte[mb == 0 ? 100 : mb * 1024 * 1024]; // min 100 bytes
    Arrays.fill(data, (byte) 'X');
    return data;
  }

  public static String[] passphrases() {
    return new String[]{"some long pp"};
  }

  public static PgpKeyInfo[] eccPrvKeyInfo() {
    return new PgpKeyInfo[]{new PgpKeyInfo(ECC_PRV_KEY, "063635B3E33EB14C")};
  }

  public static PgpKeyInfo[] rsa2048PrvKeyInfo() {
    return new PgpKeyInfo[]{new PgpKeyInfo(RSA_2048_PRV_KEY, "3A30F4CC0A9A8F10")};
  }

  public static PgpKeyInfo[] rsa4096PrvKeyInfo() {
    return new PgpKeyInfo[]{new PgpKeyInfo(RSA_4096_PRV_KEY, "7C307E6F2092962D")};
  }

  public static String[] getMixedPubKeys() {
    return new String[]{ECC_PUB_KEY, RSA_2048_PUB_KEY, RSA_4096_PUB_KEY};
  }
}
