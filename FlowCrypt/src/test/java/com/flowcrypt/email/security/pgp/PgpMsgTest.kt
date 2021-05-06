/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *     Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import java.net.URLDecoder
import org.junit.Test
import org.junit.Assert.assertTrue
import org.pgpainless.util.Passphrase
import java.nio.charset.Charset

class PgpMsgTest {

  private data class MessageInfo(
      val content: List<String>,
      val quoted: Boolean? = null,
      val params: String,
      val charset: String = "UTF-8"
  )

  companion object {
    private val privateKeys = listOf(
        PgpMsg.SecretKeyInfo(
            passphrase = "flowcrypt compatibility tests".toCharArray(),
            armored = "-----BEGIN PGP PRIVATE KEY BLOCK-----\r\n" +
                "Version: FlowCrypt 6.9.1 Gmail Encryption\r\n" +
                "Comment: Seamlessly send and receive encrypted email\r\n" +
                "\r\n" +
                "xcaGBFn7qV4BEACgKfufG6yseRP9jKXZ1zrM5sQtkGWiKLks1799m0KwIYuA\r\n" +
                "QyYvw6cIWbM2dcuBNOzYHsLqluqoXaCDbUpK8wI/xnH/9ZHDyomk0ASdyI0K\r\n" +
                "Ogn2DrXFySuRlglPmnMQF7vhpnXeflqp9bxQ9m4yiHMS+FQazMvf/zcrAKKg\r\n" +
                "hPxcYXC1BJfSub5tj1rY24ARpK91fWOQO6gAFUvpeSiNiKb7C4lmWuLg64UL\r\n" +
                "jLTLXO9P/2Vs2BBHOACs6u0pmDnFtDnFleGLC5jrL6VvQDp3ekEvcqcfC5MV\r\n" +
                "R0N6uVTesRc5hlBtwhbGg4HuI5cFLL+jkRwWcVSluJS9MMtug2eU7FAWIzOC\r\n" +
                "xWa+Lfb8cHpEg6cidGSxSe49vgKKrysv5PdVfOuXhL63i4TEnKFspOYB8qXy\r\n" +
                "5n3FkYF/5CpYN/HQaoCCxDIXLGp33u03OItadAtQU+qACaGmRhQA9qwe4i+k\r\n" +
                "LWL3oxoSwQ/aewb3fVo+K7ygGNltk6poHPcL0dU6VHYe8h2MCEO/1LR7yVsK\r\n" +
                "W47B4fgd3huXh868AX3YQn4Pd6mqft4WdcCuRpGJgvJNHq18JvIysDpgsLSq\r\n" +
                "QF44Z0GOH2vQrnOhJxIWNUKN+QnMy8RN6SZ1UFo4P+vf1z97YI2MfrMLfHB/\r\n" +
                "TUnsxS6fGrKhNVxN7ETH69p2rI6F836EZhebLQARAQAB/gkDCDyuMzkoMQjC\r\n" +
                "YLUdlPhViioPQAb/WMfaiE5ntf3u2Scm1mGuXTQsTmU2yTbY3igXTJ6YJH4C\r\n" +
                "FLB18f6u+NhZb0r97LteF4JiuTtm6ZA63ejSgp/5Lns1Z5wY7pMNPsH0cTU3\r\n" +
                "UrFQh/ghoxanSHaN1XQpaovYsOHfsWcYzAxtvqhDV2vqfIlhiL6EdE7Vn84C\r\n" +
                "QE096Gu4iMtKZSXCDU9B1XN2+rK2e+9c1nQHAXjAq49v9WUzstzjvrCmBajU\r\n" +
                "GS6Ccy3VHRel458boMNOZqvLOBCtw4nx2GFDs16ZQNZFywj1pThExKMxHlnB\r\n" +
                "Sw6tMJ0FJBCk2E9S28Buudu6sJJerZGjUKIafoSCcO8wpvixzL1s4dqJs+iH\r\n" +
                "Rv04xfHk78rgaP010NwoqHjd+ops+NxLoC9dS5PcDm9CAuBxRjf9gSJ8Qo8w\r\n" +
                "1jeldj93qiYU4Z0O87rzW+IDX6GBIcE8JghpOO+XMxN0nfb2EZpkFhBLe62j\r\n" +
                "JOgQsdoYy+iBKGSgx2uoHIvifxiq827XGCXTZmHkAwZyJPl7myja/qdiudlD\r\n" +
                "FnBtOexTnZ77HXlFC9s7/PItZZqKnIPuIKhsW/Tk8fzpUPf7Uu0tS/vCzc8+\r\n" +
                "abrpQM2ppI7O93kCKJdXVzKioE2MfV5DhLjNOe82ORWZO4uwCg2u8ZHhCOzz\r\n" +
                "MsMpAm89f+1nvOQtvBS9v45MhZdbndwPHQEg2pM689ZxHw9EgPiOhyOMFGM1\r\n" +
                "4tBZ3jYfPxGxTW7NZfryWUQxvSSkFaLsPMjxTB93T/fsJdeUDGKJR44cRweP\r\n" +
                "ATQWK/EBgx1d+jJUHKYl5aHUxdKLvwH1ikzgHuKkb4pV0yaRjONy8sEgXYkj\r\n" +
                "HoH1qfZAqbCoqraRorTEgO9QvzaLKb9gdDjEMxuzEJuA3QxP9HqHR5aU+T8o\r\n" +
                "hXss9SMFVjVok6t92v6keeUVRJYxjhoROZNUeMwfIg/EQEXACQUIGokJqaYC\r\n" +
                "4TX3GoLWa5+BTw5ChKiRC9VOyFNvq8Q2euBzSRoYPYUU7ekDawUQU+SCpN0I\r\n" +
                "3HYiJlGzDJ7zwFybeEMdv1F94F4NefROdRFcPtzrAJ4LM38zoq2YEUyK9RN0\r\n" +
                "lO2I9BFw30AR+Ps/qxtptWtzNXBohYuNNpaZt2UMcMOZhRYmHJjq59oH65X9\r\n" +
                "/l8wEk4nxgFmCjnpHyGpn2jtdMtRDrwAeHKBJ4ZQUV3SU/cgpc0VZ2rg+ZqL\r\n" +
                "7iAWfofoD/M/3bEOu6ePqcl2bKOhw8RT07CimovKUcXujp7/hsj47yNGKASs\r\n" +
                "rZMyJXT+VqMA/MWb++jOUwQkCz6dlzM8W5UC2ezlm1uIX+nrZp0LoWzq2VGG\r\n" +
                "ENbDpnyXh0W3FmVeSgwej1Fg7AJ4wdLkPxeb916UGONrUbFYRtE7jAo/h9c3\r\n" +
                "kus/8rsxMVfTvQu+tZPO7liWxhuuRWaG+YOJe2s8NYuqlyyPpvKRtGIqy363\r\n" +
                "c9j5VnfqOil1SxAjEgm7E5AHkCdQD2/BL4+hReex27WejedSHRVyQ6M8H0RO\r\n" +
                "+48eflFeaCTTWE970HIZ1hMQTf3bLEaB08758UuYVa7geF6jQmpg8OnkRPBQ\r\n" +
                "acQHgBOV1Fzf0an0uMhVw0vBQIX3XdaLe+uVUuvl00VOLB4JErCQzKDGsAMj\r\n" +
                "N2uE1cACfAEauTMik9+/G5wp2hW8JOO1mrH7lq7z3RzhJN/VkTFFSOGy/mB1\r\n" +
                "yu4inb+u5aVyJIL5ljs/NBno9b/aDOUmmiHw4my0KCQVdGNbletqfjeJV4gM\r\n" +
                "IQnXYXlQgg398LBawCNLYHkb/dDNO0Zsb3dDcnlwdCBDb21wYXRpYmlsaXR5\r\n" +
                "IDxmbG93Y3J5cHQuY29tcGF0aWJpbGl0eUBnbWFpbC5jb20+wsF/BBABCAAp\r\n" +
                "BQJZ+6ljBgsJBwgDAgkQrawnnJUJMgcEFQgKAgMWAgECGQECGwMCHgEACgkQ\r\n" +
                "rawnnJUJMgfO5g//audO5E7KXiXIQzqsVfh0RpOS5KwDa8ZNAOzbBjQbfjyv\r\n" +
                "jnvej9pYy+7Pot9NDfGtMEMpWj5uWuPhD1fv2Kv/uBP4csJqf8Vbs1H1hD4s\r\n" +
                "D21RrHerM7xCFzIN1XHhkemR7IALNfekrC9TGi4IYYZrZKz/yK0lCjT8BIro\r\n" +
                "jYUE5CODa8mKPB2BSmJwqNwZxhr0KKnPykrOAZfpArnHEdY3JE54Se6FCxKM\r\n" +
                "WOtnKBHcwHiSTsX/nBtK30sCul9j1Wgd1jFRJ244ESJd7M6cBlNrJ6GTZDil\r\n" +
                "rmpo9nVO0slTwD/YD6GCyN3r3hJ3IEDnwZK05pL+1trM6718pyWaywfT62vW\r\n" +
                "zL7pNqk7tIghX+HrvrHVNYs/G3LnN9m5zlCJMk5wKP+f9olsz3Llupam2auk\r\n" +
                "g/h1HXEl3lli9u9QkJkbGaEDWR9UCnH/xoybpS0mgjVYt0B6jNYvHBLLhuaj\r\n" +
                "hR+1sjVIIg0kwfxZfQgFXyAL8LWu4nNaSEICUl8hVBWf9V6Xn4VX7JkkWlE3\r\n" +
                "JEByYiuZkADhSdyklJYkR9fQjUc5AcZsUgOuTXsY4fG0IEryMzrxRw0qgqG1\r\n" +
                "7rir1uqrvLDrDM18FPWkW2JwGzF0YR5yezvvz3H3rXog+ryEzeZAN48Zwrzv\r\n" +
                "GRcvEZJFmB1CwTHrW4UykC592pqHR5K4nV7BUTzHxoYEWfupXgEQAK/GGjyh\r\n" +
                "3CHg0yGZL5q4LJfn2xABV00RXiwxNyPc/7YzYgSanBQmzFj3AMJhcFdJx/Eg\r\n" +
                "3i0pTr6qbAnwzkYoSm9R9k40PTA9LP4AMBP4uXiwbbkV2Nlo/RMgmHN4Kquz\r\n" +
                "wY/hbNK6ZujFtDGXp2s/wqtfrfmdDnXuUhnilrOo6NR/DrtMaEmsXTCfQiZj\r\n" +
                "nmSkAEJvVUJKihb9C51LzFSWPYEMkjOWo03ZSYJR6NjubjMK2hVEbh8wQ7Wv\r\n" +
                "vdfssOiwO+gwXw7zibZphCMA7ADVqUeM10q+j+TLGh/gvpm0ghqjKZsdk2eh\r\n" +
                "ncUlTQhDkwY8JJ5iJ6QThgjYwaAcC0Ake5rA/7nPn6YMnxlP/R7Nq651l8SB\r\n" +
                "ozcTzjseOSwearH5tMeKyastTWEIHFAd5rYIEqawpx9F87kLxRhQj9NUQ6uk\r\n" +
                "mdR66P8elsm9AZdQuaQF53oEQ5zwuUK8+wXqDTC853XtfHsCvxKENP0ZXXvy\r\n" +
                "qVo2INRNBO5WlSYQjGxoxohs1X+CMAmFSDvbV70dZVf0cQJ9GidocAv70DOH\r\n" +
                "eXBuOiXZBqyGSNjecPl2bFr4A6r5RMnNZDrYieXJOEWUqgaX0uNQacX4Aecm\r\n" +
                "KiCEyR08XKEPVnnJGUM7mOvhuGdH0ZC03ZUPqLAhfW2cxcsiOeTQ7tc8LLaT\r\n" +
                "u348PxVsPkN19RbBABEBAAH+CQMIXnc7lrdca0Rg8eLOHqbZ4HZeKswdL0Jg\r\n" +
                "lNZ9Fnp2ZPOgrnYxqeCbpRoZwr09aZ/DYiUfUgDuuJIRFzmorhczyKMOWTW+\r\n" +
                "eTTGFWIJB4ANAGie4EUhrJe4cDFxPg4Uvuvk/KMt5kLTOcSaN70kIKvt2lQT\r\n" +
                "LFzi6gCW9TWAtf5Jg+CcOoOw0tIsBcxTSL2V6SQDAS9KBJcgyhQAvbey44Zb\r\n" +
                "8s8kmtt1Xwl9bEtG2uDvKNvbb1GszeXf1wW2acwO4OAmrJXMhOscWb6eiDx+\r\n" +
                "9sIM5VoDrUG6K+72VSn9xqbEE9K5+U83qeAIb8dn+FLuKR0+auAJF19bkCUz\r\n" +
                "ZQS3wXzCm/D+PqE+BWexd7Jd8m/zg0eSzw+0hAxQ7sw5c2EdZ6LmqA1qs6yU\r\n" +
                "gIk92HufgO+fpRnUVRhD7ZklxfaG3dLIdbOs5R6DdA9z015r2VOh4S5mLJfY\r\n" +
                "rCQqEtqbtJpUOreyXejDYOCvv/mpFa81EWOMGmySvEPK7d25VAgIM/MS2l2H\r\n" +
                "+oJ7nMI4ZxgB7W4nVz15YK3RoJKAlcyy//1MAyNbH3TU8rytOKcPfCB7qgAP\r\n" +
                "6yjBGrej0IzuMWBmuuLCvfiYIqYDBgZ6GruAJhuudPXbhojl0l9z+lcM94IU\r\n" +
                "GOoGGhUIy3Zgt2nLQFB4vegY2EYUBMOO6dYeMh2QMwlSU63cnpxE/w6wby8b\r\n" +
                "RZ4GDPEBmYkCTZ0vbV34vJ72Z+LJ0/FOrTuQA8RHUJ7LMkTV5OF8oo20cE9O\r\n" +
                "h8Eyay+yYtccL2M8mpiJ9FXOyxURVyz4VaDfSXfo2auSzvUALVA/kMT3IVWZ\r\n" +
                "929TCb5uI/KwcqIMlP5Ih5hj0+0C34v5XnkNDLO8G3bcQ+NAb3ydUg5FpERy\r\n" +
                "0vW5TtgQmf04CdPJ341urINodtrUSabiOqBHSTLVaaYbSFcGF7Ur4lRwDlx3\r\n" +
                "50JqAcbp8Dl2eKMjW7EkHnTaKKC7lJwG62I3XbVg4mdkgzNAOF9UKjdHVn7Q\r\n" +
                "/DU3m67ZN5wwSruffu6DWNrDnytpl9Hdw2s9AF5UN1WfxobO7OD2vQPJ8Fcg\r\n" +
                "8kPUgnJ1OzBtvzNP53Aorbd8RDxx7rBQBtCFQN8Uq1ksUKiYNqbeRZ6vOtmH\r\n" +
                "LhiyhjMsrF0cog7r86dAAlWQJLnv+zUtdZ2LGS3aGMjAb7Y/iOVS5Q6/7Zw9\r\n" +
                "3wKAAfl7vFdZ7TFzq/p1/+xwc42WtO96XjFCtqKtpQ4sGRMUt7zUuq8ZJk26\r\n" +
                "ja94/v6iHhR9GID3HNiDLRvpQPFiK5PM5p6EI0Z5JfL5OQjvsmv/ltu5XmO6\r\n" +
                "08uXoZJM7IwPBdGZ7yNrDu0E9MEU7UW5kgSQzeGESV+jw2rXxU6wXeIAhkGh\r\n" +
                "um7f0JoKqYYOjRDLAXOtJgVAHGcto+U4XJQK4Bv7ohCpP0M+hSrEU1uYGC1G\r\n" +
                "4O6GSpN+4WV99i+JLFBya0R0ZUZfKQuU0EjRXX39WKT5LX7cUTTMHtuY3Xd/\r\n" +
                "BC/YMFqbk5+MZ0P1L1awJGkhMSc1qCrQ1OEtMk49ak2IrT+E19gQ6lR3q7yQ\r\n" +
                "B5D7ys/Z5X0ZbTsy6GK5JZE3X31of/DTbigqWLENk3oh5OaDNZ1hYUmHe03s\r\n" +
                "QuVrwD8TWqig4/KfQgFrUDLEdTZJezUsybHzjzWw5E8+5WrlBnx5PERaZzBF\r\n" +
                "wubVzAIAD9NsRGAzEF0ZBIsqzcxMQzF0jnvtl7YENQuH9ZYNuZY9h9f8uczV\r\n" +
                "6WM2HmQioFUswnzC+FNwC9Re+NNjrtdQAsVJAnn2BW4tvXlYb37foe0r8WDk\r\n" +
                "ULZ4Q8LBaQQYAQgAEwUCWfupZAkQrawnnJUJMgcCGwwACgkQrawnnJUJMge5\r\n" +
                "uA/+NA4zV+NWRNIpkyTDPD7FGi4pmFcMUs96Wzcedx244au4ixLLprSOib5e\r\n" +
                "A1UImjRWptJII6rZJCHVrB/rFJVQhSHaJQCsSd8K0N1DOOrv4oaGrL9zyzPd\r\n" +
                "ATW8izY9rzIRaNg9Si8DvULfKIheLI429RWDfeYFjFPVJ8n55gwaf28Nptxs\r\n" +
                "yo4mEWhf+pF/l8HaQtOzLB82PE4NXwrzf2MogNz3W5BMvcWZo1Vma4Iz1IJf\r\n" +
                "HdNlZYJO1vMC7u/7JYAztyH50mXT9Jh6U2jim5OElFRNEUh35E1L2G6XzRdO\r\n" +
                "JrEXbghF7EO+iekIyRScf2pE+vNBhL2iwnJs+ChgFDFIGnR+Zjwl3rG8mux0\r\n" +
                "iykse5vOToid8SEZ16nu7WF9b8hIxOrM7NBAIaWVD9oqsw8u+n30Mp0DB+pc\r\n" +
                "0Mnhy0xjMWdTmLcp+Ur5R2uZ6QCZ0lYzLFYs7ZW4X6mT3TwtGWa7eBNIRiyA\r\n" +
                "Bm5g3jhTi8swQXhv8MtG6eLix8H5/XDOZS91y6PlUdAjfDS34/IeMlS8SM1Q\r\n" +
                "IlBkLHqJ18viQNHqw9iYbf557NA6BVqo3A2OVPyyCVaKRoYH3LTcSEpxMciq\r\n" +
                "OHsqtYlSo7dRyJOEUQ6bWERIAH5vC95fBLgdqted+a5Kq/7hx8sfrYdL1lJy\r\n" +
                "tiL0VgGWS0GVL1cZMUwhvvu8bxI=\r\n" +
                "=2z30\r\n" +
                "-----END PGP PRIVATE KEY BLOCK-----\r\n",
            // longId = "ADAC279C95093207"
        ),

        PgpMsg.SecretKeyInfo(
            passphrase = "flowcrypt compatibility tests 2".toCharArray(),
            armored = "-----BEGIN PGP PRIVATE KEY BLOCK-----\r\n" +
                "Version: FlowCrypt 6.9.1 Gmail Encryption\r\n" +
                "Comment: Seamlessly send and receive encrypted email\r\n" +
                "\r\n" +
                "xcaGBFn9GlIBEACiAU8yhymNq2lTxEG1OU0Xka9tUJ4A7wsDhHNnuhxzjVP8\r\n" +
                "TDnpWb+kQ7pDgj4SEjXV5NAKLS9ISRsizxEvwo8HWulL0kmmlaESd5oNwc3+\r\n" +
                "O4CxX3M9oNDaEHXmsphWyvBvTxdZW3d5I9dT4vjJ/p7AznY995bKhLCK7Kyo\r\n" +
                "J6Le+H7I8EXUfNBIkK7AUmhtzaH2UlhfBtJl3+VK7mAje6wgvf4bz+xsuZ/s\r\n" +
                "GlQAhQjrRax/zjTxSHdEjBJ+l2gIvCnkVe6i/BcjqLQUvHJsgzaKr+3Ri2Qs\r\n" +
                "AjVL3MtsNyUha2QImkWSP62J28AGSgk556vd9COP89dxcmhXlmeTM40A29Gc\r\n" +
                "xNzoBUDJxbX//gk1VVXhOA9/Bk6JAS4T+m3IftK3QJNC/y+SnqDV9xwAl4KM\r\n" +
                "8qBweUtFJ0X2C4DbC9EIP9F2Sy2jWbM9cuaTD21mjQdOU5cbWkJV40H2FgEH\r\n" +
                "cbKB9+GlMntg+tPUFlrIJPSKhDUBCym2zUbkWkz606q5W5vpSUOu+3GiV2XF\r\n" +
                "eGvv9afnOoo3rLjVW4UimcEDLrxiEdct+oDTI0XRNTLIUFtZskdEUe7pPoqW\r\n" +
                "4+TPz9GxUlfP9Csi1pylgHHclnE7s/B+Z+tjUOrhIayw6j0dYtl0zBhMe14J\r\n" +
                "w53fO/AKe4hthVYOH1oj6zSJKeEwJYe9F8ofsQARAQAB/gkDCPoUlT4Nfy7g\r\n" +
                "YOv5N6DzjrDheK4xCLNt/q6seg2sLZtqYIcIU/5ANxkdhELouC/n09dOBNmc\r\n" +
                "QpiSInCKYGnPEHqqBacFKEwZIxH+yUAz5wrNPK8H0vdfhGLg7ZYzpUB6DVJa\r\n" +
                "Ho2oNMu9UiYg/B7d42KbsjJuTqDBs/MCQnvQp57MLH89T8AoM1+C2sPZBBPC\r\n" +
                "5JE48NRlkdHj4OLultAKA9Q4wHdN9kpneQ7kKW+OEEn6z65tv6aFTB88Cjcd\r\n" +
                "KkvDTWnGo2k8D09GaRwKeCU3rPGEgR6cLKVQteY8vN5tucZhb1uOQQP+vsK0\r\n" +
                "B1tZKTUDXJFIrfBAU67YMwGdK16BSp7SLNxrRcpxBrBPzF0ol/F+o8wOPyE6\r\n" +
                "40N1K0WExMdYQjzGThajw2gIR2CyMvLCFKojmtjmp2p93kzvkG4musVayQRU\r\n" +
                "YYseCyPldCUzqAzd9wQwPY7Ym5tpV7qqRXfNrkylEBU6NwFX1F6bzpXZfExu\r\n" +
                "/1iisYGeLNUq3H1nKpqUdGwf0qq5DuE4eKgFVxB71LRtXj/a9m4BY1wgSe4C\r\n" +
                "ZVKxpDC5BLOJClQy684kKdeVpViXTovn4OhbiSgTzkR0Dhd9TOiwZlnnIzuC\r\n" +
                "+5zhkcfpnquPRncqfRvJorPjHlkEQdzswh6Zp77EM+71EBVXfCfADXZk4m0m\r\n" +
                "8XOQ1W23SE9qYZ1Is+PgArCAEAwPEp+nhQyFOdvR/q5gw/Pp93bsdXCFLlNE\r\n" +
                "XZIL5NkV3ypZdLXEnEWclGG7b641kGCLNi1TA/EnThr0JMrlEgZjgfFDrt3V\r\n" +
                "lyfEdUsSRRNFfGXp1T7VzVzp7AaCS8vvnj3uh/UeekWFC3YWQz+4LX+yC8zV\r\n" +
                "/Y621wBkZUVllKXECDzkHXKhAa9wRnon5F4X8wyyeNo5+tuezpoBqPkwdU0r\r\n" +
                "Pg6uf/Hxukd5sYKPUAlI4MQiz1M0dTXSRJBxMyto3j3zPb/rqMssta/hKoZq\r\n" +
                "KDkjyyEEJQsEB0HDmKCQrCRKX9cpJmFOJ5cbi4KoIgFAHYxq5JkOTPxuajxV\r\n" +
                "8QD2RnklSJIXZn/q+RkA45KXat1cMTmTbmBacnKeJZ2ajAsh/pWNL7UtRHNu\r\n" +
                "JpeFWQP5lyCGZl/C1yAxRExSqNGdkj+oUeIk0avPHMR9vSnwuam+t7y2lZxv\r\n" +
                "an9Axco75rFMs6hGxEhMRhKeluWcbwWiA55QIgUPS0qCMscE54K3Nw7QwpZL\r\n" +
                "9UrSWFTxJQEUnT2VJGFNv69XYCaLIWsgk7hMLIS6oP62TU0o4dNgig4I3LU/\r\n" +
                "WjFD244+B0kHRr9adL/U7vXnbtTR2oAzJ2816/98Fldi0vXIoCmEI/30Bq44\r\n" +
                "W6ZViQ2MFgNf0x9xnb9TEi7T7ElGeZzJSHJEHC5YqRKJYduvESoi+Kiox2ts\r\n" +
                "oSjLsrVh+itccVQ4MHJaW3jd0yjZg6wWMPVkfC3nzjVsvNvWm5eXcbSgajtx\r\n" +
                "W/JV1We/ykm8a/a63farOF56MnkWrHgz0HFe9bILPqJ5fGzJR/ZfmZVS9pdi\r\n" +
                "9jt+VmREFqhh5CUdiG+PxqeWlPc+IFjgAF6b/DLhDG5Om7JiDuZat3ociVxZ\r\n" +
                "C6kUittvkVChrmeRjth2vvbVURmjPea/x13U55PLoWxkiVU3vD7/Hv/sKhSI\r\n" +
                "MI3dfV4pkVvOYUEqT9B0c4TGTKSPdf4eLwbbBOuLCQoWiElM8n3XZ0maVWkE\r\n" +
                "LvGktJJ5o2bH2gRaPlSO48yNfJSXkU2TvESpBT4kPxhp/Cye13MldTyxY99u\r\n" +
                "aLZS9RgQf3RgikYw1+VYTTwzJpzNO0Zsb3dDcnlwdCBDb21wYXRpYmlsaXR5\r\n" +
                "IDxmbG93Y3J5cHQuY29tcGF0aWJpbGl0eUBnbWFpbC5jb20+wsF/BBABCAAp\r\n" +
                "BQJZ/RpXBgsJBwgDAgkQf95oVUiup4gEFQgKAgMWAgECGQECGwMCHgEACgkQ\r\n" +
                "f95oVUiup4iDNA/6AqicHC9UEAA7UotXGzbJtFYL8t47tcAyfGirQZvi+JqG\r\n" +
                "IIDYH9m44WuFsK2con8ZQgzrRDBgwZgpbsShC74ZzdiUnRPyuGEne8SaNvVw\r\n" +
                "t41rQZxrz8f0mwVZq79CjjCjHuXp+iVLof0bNDYsSsmfykrPd1rEIjaBkXzs\r\n" +
                "g8kdX6If2KPa14K2z17KTAY1TTrPY6CUtOiia+AYKr+71DSoOWxWeYFRPuFE\r\n" +
                "HuFMquoN6H9buZ7thAiPOmNLFmspox88HXn/mnUvebTGsEVJJOIpLU2aADHC\r\n" +
                "vp+SOyzYVEO467uT8F4SB4BFgdIvbO8lC88TMobgOoLLIY2WJizePMKCP/cP\r\n" +
                "1cawC7Z/Vll+3vTQfKOb0BoDoWRhlAGYefcqCWtKaORwhx0cz2V7mnP3YWMn\r\n" +
                "jYztE6xyGy/GtbyR23wG0XVIVnE/FgPqQHXiXjdtKO6PKcU1DjJdJW6o+Y61\r\n" +
                "hrZWGeBSK0xv3j44XhcakRUJiJtaDysfBMqirPzo4srS51q7iKRNTOvsJD2o\r\n" +
                "/R43m/VAD7FpRoDHHxfLj8iuo7OWI6NjMDP6TvQSGsZJO8ZZJnTa90hvH+8c\r\n" +
                "hVRtgvEwcpUfTXNIJBNvgvT1t2fG7ROb8zob/bRuuVe5wu4z/w4Jscu+ISJR\r\n" +
                "owKpTyBF7qxCCqdjqDcO/BmI+5slIINJc2Hu05XHxoYEWf0aUgEQAN/ROjAi\r\n" +
                "waY6HROvsCa3coNWQXFYzPCh86dUD47diBgICxHla9HXxPBrf2OqDfIJ7oXp\r\n" +
                "b4UyWK/7qbZzCNnN3IR8Plv3TOAVsL32rF7in4HGUparizC72uhRFWmr7bqp\r\n" +
                "6v1VGzwWlJc5weN/G8wit4CakRnUfm7NwvjqqXxg5ONbl/Z6HL75/0fG0wx0\r\n" +
                "UxEz6csAaqATyN4i56xFtcZz0K9kSViydYRBxL+asgLboYTzbzZtqk4ixmO9\r\n" +
                "Bd1WE7uSJAp8s0/hEZGZxl1zmFAaiIRt5+Et1nnQNXmB+AvDDCXlCmEnavct\r\n" +
                "0DRgfOM3KQlpiknpFzNPNkW2n8/A30hU85/qpBXKoRPt1PaxyCRDWaWME+QZ\r\n" +
                "zic+t6GQvAHKJ0bJdiG6i7AVhA4H4/YWWbK+fPIiihTl6gUd5d11f5ut4gon\r\n" +
                "EnI25uPUULQvZzsLUiqQcbKeiWm62yrBk2JDF0QrQUsngtDYDMjMYD9NrNz8\r\n" +
                "AymoLSJHekPuy1xCyw4b0REmdR5waDOadW6uWKEMJ8H4sB2nJZ6M3Y6TsYS6\r\n" +
                "bzxaAV0biIGlrMeMQiyBxiw/vDq7kca4W2zxy/CzwcV4E4al4Lezaq1Fl31W\r\n" +
                "x2nNwJ4jylqOr3z0w6eUnOhSwDR7iYTmQrLTYgwFc2+bgErFCY9AeG8dIoCf\r\n" +
                "tJY0GxFOSxByI3iTABEBAAH+CQMIlV15v2w57sxgNI2CzIp3RK3PqHyjIQge\r\n" +
                "tEyITe8BN+8X3wTbuq4yDnU6dIpyJZVwHm6huiTIihtBnOS7asa7sbO9EZMb\r\n" +
                "59avNd67dxDnItGxGOkL/P6kqI7JHmtQl223JY2T0JK3qve3jqW8bBrTnLUB\r\n" +
                "SgfAZWoLVdvwVUJFnTOHHIIV07v+byK6UL8+jwRDtyq6t0M3EqMf/O/lqPnB\r\n" +
                "zCYK2hmtZ7PNq3vpvOjTAuO2m1a/Pik/7UjxzSGJS1oEiSJfd5gyJKFFlBHf\r\n" +
                "wA+0bCs3ySoylofTiK7NrljuXYFub5MQ7n2zHAiCTm0ba9frGvE4L6audOGn\r\n" +
                "o5noog/jsYvwsGZiCeaHzkQymMeOC26tF8gfmEQZH0/sNv1+LEpiJQWs9mVf\r\n" +
                "JkWBnjoPpjrZ7El1wRjhTblp0regg9tAqo1EbApEBxWwbzBo8ddg0E0My+D5\r\n" +
                "HQ6R7rJGDx+isCnMwdObtJAhUCDWPmo7h23nKVct0MOrJCzgEtrkR6rMCQoL\r\n" +
                "VqW5A0jH0VCVns/JUoetFeQP2T3jG/xevzuHj6pL126yPmpP/gq5PZxuhkyJ\r\n" +
                "nzWQSJdILZ4saEEMveldurTp1TgIcpESIiUiHFeBNjD/r5JuwmrS5uEgDTov\r\n" +
                "auS3KVnU0Xbr9JtPdG8CYLIDceH8R+tIk2X1Pkb8rrZiee+K88e3P/vqLsJb\r\n" +
                "RNYHdyG3d9MKig4uOWwzqOa/44tVDKtpr8JkLnTOHXh4fkZH5KOoEBDhfebG\r\n" +
                "offVRLBfW+/QaWZqXRzHip5K59QG1ZonHjzd/p0rG9QSWiA71/e16yCRfc+x\r\n" +
                "FQOV+hH9AHS4iqVOHNZGdEpGkD4S5d6DMmoQns+L6c02dJNe5lshk1vVoPEj\r\n" +
                "bn70IXqQx37w5rSvY7fV72usQAFXOQRDhleFbGHk3K89JM1RKvfw80wRh7aR\r\n" +
                "Cc3dSDaoI6tkcrHBmwnuhtQnyvQC1BCLKTcaXvxEiI08cOaQNNy3FztjAGu9\r\n" +
                "f00g/A07IjlYypY4ThdYeFC+9HEu/Lc2SeisRCBmUY72UMPmXaFZksNJ+KqQ\r\n" +
                "jSq94KGhcUbBdCePW/628rmd2dV57HfY8r6MN2uE3az3ncK9tPG73vgkD5ge\r\n" +
                "3Mu5no6PnWyYgdsNdVbEb4AQwtuyeoqDWtKv2nP4HZYeXWVd4Fndwlf4syWN\r\n" +
                "ULAF9NeCxc+8RlCGT1+XNOxr0El+EHhkp9cJixpQmuh/DkG0Ys+uyHweztEA\r\n" +
                "K1lor0kRFU+nuG9ZXRUo+BApGtujoBS0jT2n5TBhnQ+qWwYgdLrzYkRJw2Sq\r\n" +
                "mYZ1CQtg3QkTaSRjvp4virFx60cgM3slBR0AHLUhxcV8HXgcWz3zCrbaGLIg\r\n" +
                "P6eL7UqtWDCqFMZRFZUNR7MT4YaRTHGgziRFLVchZXT2MEcHpxXD3SoVmdJp\r\n" +
                "/TAZKZjlj+JNiqrcuyyprCr6/ekXRIy7la11czcizLAz4eX7JR7rNmpVo8te\r\n" +
                "fXb1oqyg5gkiLanjR2uV+WsdacHT+bQsTD8ANWA87jZiqKTn6nUJiXELqjzv\r\n" +
                "ot9lGsD6ek3ZL0ehhOqNsJSI3VunAs0LuTMqSqKZwHXN3eWemMzNMzXJa9ez\r\n" +
                "tNb5yOZAg7i0Y3gAaDilU7CK+aT0c9oBWn70jrv7rhksFPUaISp7tyQBvp7l\r\n" +
                "ltkut9+GP0QiNejoihTLkv3p82rETNtHi0Zyrudh5glTlTA8Iu3pOZDvxNMy\r\n" +
                "zztusW3/vWekkEVT0YkHum+NyCpEOmHZBhJ1ZE8PbD3OoIcMnSoRMm+0Fm/i\r\n" +
                "WSptQ8LBaQQYAQgAEwUCWf0aWQkQf95oVUiup4gCGwwACgkQf95oVUiup4g0\r\n" +
                "2Q/7BfCwTimOBq7lsoqlPUgww6E1ubnZJTVmcyr2Mt7IEiQxTK3p7Kx38ZUz\r\n" +
                "pA7AdNyR4fWeaWy9ptuhGFH7STpMbgFvP+C4KKnqOjqrtvdTUlYdP/YI0Dro\r\n" +
                "j46j9wgK3MMATvp3af7JrxCs0dWlVfTrHJ6NoHT2aEGtmN9Ri5qQ4jlIG6hh\r\n" +
                "l149o+8OWdMPdy3VRXsA9v7LC0PfKMx73Y0wncTuLfhgc8aZM8tWjOLnQXrH\r\n" +
                "nIfw+RFJ+WKjCRbdV7mPc90S1VkKC6tVlFKCFw6QbFDCMoP7eIOyEz73ljcy\r\n" +
                "EXPctOmxgVPMwhOOpwMFWr/Dg7WXA0S9WOkU2nyF/3NulRfPSlxIG43oT2JU\r\n" +
                "NLyPwbN6nUhisHV//n2cBMFry7C0lMgsnwy/TDMF6XPuxmCVA1nHfopGML8f\r\n" +
                "DZp6tRQVDMWMjdlqj/j0cmUSYuDsrrm1RBIzVPP0jKbjliMumYjOP3LpLiRk\r\n" +
                "NmpaljVGpOWs+otyAjoYzDC1XcS2JAWQze6PhvWgQGbI2gmxszlykjZmnSCD\r\n" +
                "11Op0uhB8jilCf3gxRk353eRpFpSu7+x/5CKsvRoL8+o/7HqqJDe1+J7nO8T\r\n" +
                "k7m+liNbDUUHjyLsNrpvmKdeJwZOuiM7o578GvW1c957l4SPdFgy57vDkMgH\r\n" +
                "vzeD7/aV7tSBoODcfMmAS7jLnHo=\r\n" +
                "=OKth\r\n" +
                "-----END PGP PRIVATE KEY BLOCK-----\r\n",
            // longId = "7FDE685548AEA788"
        )
    )

    private val messages = mapOf(
        "decrypt - without a subject" to MessageInfo(
            content = listOf("This is a compatibility test email"),
            params = "?frameId=none&message=-----BEGIN%20PGP%20MESSAGE-----%0A%0AhQEMA%2Ba5zJl" +
                "ucROnAQf%2BJc3kkQPIko5gnq0bN510e16pk%2FBNq3w00BWZZmqe8QZ3%0A2CDi1i8mJCTf0ax9z" +
                "CjJmNEoK4sonX88ZtQ3nDX819ATeu8gi6cWTaaTrdtfI5wF%0AGoD3IgRiwOGJf3NAUSa8YB77%2F" +
                "px6AL35je44uXHvstmmWrt4LMQBQaRUGHG51vxf%0AQKNx9hBHLOv83wGjjKoDOByb0Lf2sGIlECg" +
                "eOHGfowKG3fH4NNO0kWbaLcVvM9Dh%0AgWjQQWWAWhZCuFmpYdIktYzC4CN7JaTRdGbyuK2syrsiW" +
                "yc1tty%2FlV1XM06dwYO8%0A7xgdXTDbmVwujEtQJW1bJuOoI8DiuRbYfEgGSGADmIUCDANLWi%2F" +
                "85i2VAQEP%2F1qR%0AiYLG5IMS60KJf89GK13PNeo1QzbNNYrNjxWyiEZOy7n0qZ1X7JWfGrRSx2W" +
                "qtesh%0AvzY5Dt%2FWQWVES%2F4sl54GO8Pjlhi6YjIn3wFyZryftOF4eXjoQ7dbbpoOsHhOizcD%" +
                "0Ap3l4zXPRng8hC4gF%2FZ6XxCsFRHLXgDRsJKu5bZ8VEJvK2m1soG%2BCDl9s%2FDifjf%2FU%0A" +
                "JVc3DWh7lQPGy%2B8TxkvHtvaD1ZbNSjOIfdmsybBS3Hk%2BSoaLb3MI%2Bv2clHMYnSKs%0A1Z2z" +
                "En21SBxrLd%2BYKWD5mBE9UZGyarANvvbMkiPGVkHzzUrfu6NjF9sVKoNLDJmu%0Aegjr6RWNv2Cr" +
                "Hr%2BREQWRaQ4004Xfu2WRZkcZH7DLaOvIMlvi8mHNW1EplL2SrvF9%0AoH7YMev0j2x0BLEkrOWt" +
                "FfRG7NpgMU%2FO1bDz3DD7uDHIgi32KJ%2BUhSYXqiMOlIPK%0A8wB39mCqgY1vD5bkw7l%2FVHX%" +
                "2BfwU7QTAK2Lg7%2BUGD29VmJhso46Mpz1pbL0HZiuCY%0A9JRr1Cxi%2FXwKWXgng8ijIUhQ8%2F" +
                "sDdUxuRIx%2FxgLCn%2BNy69MrjZnXE2T0W5%2BgBpuX%0Ac7KUdJwCUEkdiB%2FWlz4izdPUCBUn" +
                "c0QAqCt7Ixx4S4Hn%2BU1lNfrECqJI14kbf27r%0ALmLiZqEB5WJHLBtUkegyFWr6WwHmqQFxtuu2" +
                "Tg%2Fz0ukBkZDzODNz0eVQANEb%2FkWn%0AxaaH%2FDhvkx%2BDxKeyhi6LDfAtU7oOOo8C3%2BiT" +
                "Fzk%2BSsr2Tl6Mb6fuSSxxVc%2Fi1YZb%0AEOWEuw%2BTLGhH3nzWG1reM7N0q7lNVy5mz3V9cXRc" +
                "vRUj7wYBhxf4LyBRtCq3lOUl%0AhfHf7U3zk6ZpIUCq146CbWVAy83jnKbJwzmlOCWoy1rVfbRg6%" +
                "2BemShml3ugOCjfp%0AJVViuW33ZUEGYIeDHa8CihGn3ai0aN4CHQiuDe%2FA9tljFseYi%2BIdfV" +
                "hIz10VRPBO%0AbRTW%2FnFDkVjI9E3Bd%2BH5%2Byj4LEbGFYfcSHMjgoMjS578p5dmbl%2FVeNt%" +
                "2FwS9dxPF8%0A8iRV2tcSu5HbLhWGZJ1l%2Bcn6N1PwW6Rs9NrdfsMD7QNsyDU71hOz10asOgebxY" +
                "NM%0AgFhhw%2FlxHig9iuNwO8GE0HBDmRv%2BHKxLXue0pHPWt9Ut%2FmY4r%2F%2BruXloxRsU8g" +
                "jG%0AhzSamV5IdvfQy0xCog2bWDCL4rCngh1IkrFi0CtNNl1mPskhoZqMZjso290yNaUc%0AHeFId" +
                "yPyvjjxyoHd9K3BuXx6fPvYbZzFRz9YikMqHxz6AyHAiMJnl8OPFH6XOTki%0AO1liU9LI%2BMsLC" +
                "meDqGliNap9VMvpBkJK6lWoC0RDtqHM48sI4BqHBgW6nUwnGv5H%0AtKbDTgFfMZw5c%2BklOWIUH" +
                "ME4eFNyRej69uoofFyb2rNjXBqvKlL2g0dUXbm2nmYG%0AUW4JPHWria6djv6zg0h037c%2FP6%2B" +
                "DhVdm2O8in8b%2BBgqsdr7ChYPp2jUX8rouFNwd%0AU4xAXYo7iLoDN7AXHUb%2BG19qrx3c%2FXB" +
                "rb5msVllfjDfKspX9ftTBukl1%2FJv2QdE0%0AG0kEVzAVB3amt9KHNX%2BfMC28rBla60gfxmpEJ" +
                "9Q7fZCAOqTJPPcS1D9AKVm3wpoh%0ANWNJXYstblVGNBGuYeJuvHyjcGfs23RPgy1PI%2FAqJJcum" +
                "UCcGb8Aa4BufsyZLw4L%0ACkN6yaCuw5DdmeNklkm%2FNVDJJJpvkLYrTRr6V5VIeO1usvmTYwAg5" +
                "301DjG%2FI6qP%0AVRJQ7GeUXB9G6r6g15KbNfIALz0SUt6wKrFn6H39aVlBXzFO5Y6EmmD%2BYap" +
                "fJH0o%0AhOHiIbWHXqU9rPToLC2Pn9WDq9FIUMx%2B0wL0En172e2%2BUOfEoPrcoykVFXemGCVp%" +
                "0AIbB9HsIUVrsYyqvs7HLQfMdR%2FSjw%2BsFilKzIIBFjgNWHiyOwshVMkHCUHohcw6mJ%0A3cx0" +
                "1xWgJaG42ggGSYvkb4BvZEfgKmslwIV79pbwQxuayKNNqCpPwHuqA1fdlZ2E%0AhR8Dn%2BShq2eP" +
                "5lStlK0V%2BGYT9fBcfuqApdUKNmJA1Pks%2Bj9h0CzseWYA%0A%3DHRui%0A-----END%20PGP%2" +
                "0MESSAGE-----&msgId=1600f39127880eed&senderEmail=paul.ilardi%40stresearch.com" +
                "&isOutgoing=___cu_false___&acctEmail=flowcrypt.compatibility%40gmail.com"
        ),
        "decrypt - [security] mdc - missing - error" to MessageInfo(
            content = listOf("Security threat!", "MDC", "Display the message at your own risk."),
            params = "message=-----BEGIN%20PGP%20MESSAGE-----%0AVersion%3A%20FlowCrypt%205.5.9" +
                "%20Gmail%20Encryption%20flowcrypt.com%0AComment%3A%20Seamlessly%20send%2C%20r" +
                "eceive%20and%20search%20encrypted%20email%0A%0AwcFMA0taL%2FzmLZUBAQ%2F7Bwida5" +
                "vvhXv5Zi%2BqJbG%2FQPst11jWfljDQlw1VLzF%0Aou8ofoIEHpvoFgXegZUnoQXBmlHGD%2BXLs9" +
                "jG%2FTV1mtE2RWq4hDtqiTQ6rEIa%0AbrN3Nx77Yr%2B4EN1aKI20aTLEPTIjVU2GH2i9DAmjHteB" +
                "U3nkL9Z3yecB8Pn8%0AEdhpCRY6cj2yrhJ5MPwmXrus9OFv39wA2DqYpqW5Be%2BKD8mipZ2CtJo5" +
                "xtin%0AaeEhpWSDsdg26rjx1nz4dA0NcFzZK2p%2FBPfPIFzRvmoXoWFigpUnwryEoCqX%0A%2Ftg" +
                "mcrv7PqiYT5oziPmMuBc1lb7icI%2FAq69uXz2z6%2B4MJHOlcTEFygV36J%2B1%0A1opcjoX%2BJ" +
                "KJNn1nvHovBxuemcMwriJdmDj4Hmfo4zkd6ryUtGVrMVn8DbRp6%0ATWB%2F0MSE8cmfuiA5DgzdG" +
                "brevdL6RxnQDmalTHJ5oxurFQVoLwpmbgd36C4Q%0AxMfG1xEqFn5zvrCTGHg2OfS2cynal8CQDG0" +
                "ZQCoWwdb0kT5D6bx7QKcuyy1%2F%0A1TXKnp1NamD5Uhu1%2BXuxD7EbvDYUWYh3bkqgslsoX%2BO" +
                "Ul%2BONdtMD5PswArd5%0AKisD9UJuddJShL4clBUPoXeNrRxrU6HqjP5T4fapK684MeizicHIRpA" +
                "ww7fu%0AZ8YtaySZ%2FhoOAKWsx0rV4grgJV7pryj4ARBRa1pLL9rBwUwDS1ov%2FOYtlQEB%0AD%" +
                "2F47fyD%2F6BvepqWmZXj7VLl2y63eE0b%2F6hf5K%2BIzv5A%2F%2B5l%2FEnjFx0rq%2BqeX%0A" +
                "6hftYZBUAbbBvKfxq9D5xsWg3tnhFv2sYIE3YpkCSzZpWJmahHwQOVNT0ASw%0AgbO25OiTPlYPqf" +
                "SkGYe0palbL%2B4T5dLOwVilmrZ2bQf%2FrLePwA4RQpWDPYio%0ANDU0Xfi7TQcHQrZTpwFbVzNP" +
                "XgCHnQkqF%2Bs0v8RDJHnt9vVs2KEpi49V%2FYgN%0A%2BgZnZOeADL0rbre%2FPrIck1YSjZLbrW" +
                "tQVk4%2BsCf0TjvixJ7MNjA4NgdZPo0M%0AHke%2F9XBFie3NiZaW%2FcEIVZ7WnjB3IbhkmOMJd4" +
                "LgdHKgmswJwCYm%2BXvpOI19%0AFzU1vzZmfOA1nEJSuuCDNVUoKYIQA5UEYJrVJeGnVN5sU5jkdl" +
                "X9xPtYceww%0AYFmLisuf9Ev0HC7v27KwYQRDPNYRA8GeK%2FjY6aZdg%2BVccsnzEigdYL5Tm4JI" +
                "%0AZrxp%2FG807bZvt0yZwWh0gpWOFgbVgrm4Hpji5ilDyulZSW%2B8nJxB5tDoPzL4%0Aj4w9mal" +
                "je0c60GWNtiyCPLURyN63C2q144UpQjSU5r66oP1yF2A97aXKbf4p%0AqO7cSNWEOTpqJkJrNFVKQ" +
                "dWvXZ%2BmvW1PQFmkkwish2HiQIXmWb04uV1pI8hR%0A6YWk2ox9aZiJ664MpncgyJ5uIMlzVfYrX" +
                "%2BAZRtBW36RgCTprIv6l1M5NcHMy%0AzEscTaSY%2Fe%2BpM5HzQKSzX%2BzHLa5kk5L7veX%2B1" +
                "G33saiqSJ%2FfK13%2Bk7qDNZQD%0Anbtaebfh2JS0Pdbub6FUFjPHR5PydU9ltuppGEeYrOe1Sxw" +
                "iZ6BZfIXO2%2F8M%0AhA%3D%3D%0A%3DB%2FNE%0A-----END%20PGP%20MESSAGE-----&messag" +
                "e_id=166b194b21a0997c&senderEmail=human%40flowcrypt.com&is_outgoing=___cu_fal" +
                "se___&account_email=flowcrypt.compatibility%40gmail.com"
        ),

        "decrypt - [security] mdc - modification detected - error" to MessageInfo(
            content = listOf(
                "Security threat - opening this message is dangerous because it was modified" +
                    " in transit."
            ),
            params = "message=-----BEGIN%20PGP%20MESSAGE-----%0AVersion%3A%20FlowCrypt%205.5.9" +
                "%20Gmail%20Encryption%20flowcrypt.com%0AComment%3A%20Seamlessly%20send%2C%20r" +
                "eceive%20and%20search%20encrypted%20email%0A%0AwcFMA0taL%2FzmLZUBAQ%2F%2BKvSE" +
                "D2vb9fJMQd6lRTh0idC7srhg4ESSf4ggCXFE%0AdeOq2IkV5dNhgWGGawFVVUTewMh3L3JklDoONl" +
                "atBthc2OGNu%2BFyu5No7hhG%0A3Jq1GkNwCqex0%2BG%2BGVhlZfN2LOAx855H9m%2FAGxYo6KLU" +
                "%2BROmPZV8PZo5YJPr%0Ar8TrhhfHF%2FPG4ZmQIcuvPI1e0ivgF74wP4cG0qaPEacvSxQ1ZuDwzd" +
                "qC1kGv%0AseOTJEhpBG%2FD8YfbzUXVrX4GiOzIu2OhnlKfU6c0BJCTz%2BqmQRqYOZXLvKgd%0An" +
                "U0RzfLgMsd7Sy1lCpld1syY3bT4l0FIRWUtVx1NrJ7cluicEPDiqJsEZntS%0AYy1ViiRZlnk2Xvx" +
                "1Qpsh7fifUS8e9gfwPevYFhZ%2Fb6SeqpRFRDFGa0uP9L5C%0A%2FCcWqiUaLUL8nF51CYzfIMeIE" +
                "GBk0TiVUAn19mkQTFbtbIB9K3uQHjFzgnrL%0AnLaJ08Eme5NugtJMUIW7bgo4CAddRjj0isFsoes" +
                "Uv75%2FmEsHJ7JRPICnWx4b%0ALPKOyP0anN6TYDgTC6IqvMOoNi0ZPEIpmGmf7ZOWjR4eUT%2B9u" +
                "BmBHEPwGbLQ%0A85Mcjy1C7X%2B0uUkIPsqXgF7Ya%2FpwTuZ8mDtF%2FFU3kR87y3jlDZ%2B3ltq" +
                "%2BY%2B5A%0ABJyMGXGf24%2BSquE1Q%2BONIzBwBqwXuYvRJwqA9vOtZ1PBwUwDS1ov%2FOYtlQE" +
                "B%0AD%2F0R6LMWFQHZQCIFkvXcB5r4X3J68tcLffAIVs%2BJnoyR6JECUuCZJdKLc4Aa%0AF%2BA1" +
                "5GKiOnf5Z8RIg3Fn3nXuyN5rlWOu0yOO%2FXrnCSMHiYErTLUO6%2B6V6%2Bby%0Ai%2BPOAtAWpt" +
                "nJ7rGSAy17ZgIYD9WNPdX8Bv1fWEOJII2rj%2B5CVyBsOZWrnlnP%0AHjHOQ6gHop7bnQlrpmpA95" +
                "PLhyoW1LkEIoC0jgrGF%2B0QXRqEfdwpQBCklZyL%0A%2FWAsG2GJLrHUgQALgpTys6%2F5P7VP%2" +
                "BVSOaEnOJJExIZPkRVRFzWlYq1avgJWw%0AEFGmKeg335%2FiThKBFQ8JsH9U22G5DD1BcfX%2Bqt" +
                "m4n640zC5pHRpLJO6ggiCJ%0AZA1SCtq6TBSF1FTa158ZNgjkiGZfS%2BoZvrMW%2BS1691vMmJrw" +
                "qiRlPg9PXCA%2B%0AouGrU%2F1FVyRKGx1%2FUki%2Fh9SaxDX%2F3uHOOwJzytNxGMJP%2F4Y1Y6" +
                "hbDwDzcrCM%0AFlFHXiNbfB3uxiHD9wWHE44z91MkqOb7%2FajoLXA8J8U3KJGFa%2B8JkZleRVnq" +
                "%0Ar%2FUT8ppv0%2FozWzV59mTulYzRdIPSy6r4V0bH16XGwZtHVrljOi4TrkExB9cS%0ATdcX96R" +
                "MMYpJ7p7dGcxoHaRBY120BD%2BsJ51jGi%2FYupoZBdbg7KcOAEelD2%2FF%0ALM1LzR9f3HUaYyK" +
                "vdPL%2BC0OwINKCAZBPShfECZOiqrNWgHLWddAdXqexZFLH%0A0y7td11E7UNcCZegIlwOYksW7yu" +
                "uCZ2ZLLnfx%2Fu1G18nKBCealqNkaow%2FPj7%0A4q%2B0UYxfZnAl%2FrFuTK9ndd8tWMSm%2F6x" +
                "zWEbqe%2F8NKJrCwk%2Fnu%2BpvF%2BMuRvf5%0A9DuzZFiNRQSjSxSYvkyLuw%3D%3D%0A%3DvxO" +
                "j%0A-----END%20PGP%20MESSAGE-----&message_id=166b194b21a0997c&senderEmail=hum" +
                "an%40flowcrypt.com&is_outgoing=___cu_false___&account_email=flowcrypt.compati" +
                "bility%40gmail.com"
        ),

        "decrypt - [everdesk] message encrypted for sub but claims encryptedFor:primary,sub" to
            MessageInfo(
                content = listOf("this is a sample for FlowCrypt compatibility"),
                params = "message=-----BEGIN%20PGP%20MESSAGE-----%0A%0AwcFMA62sJ5yVCTIHAQ%2F8C" +
                    "kcWeLmCy8lvANll0KbA9ymThNOmZjblBNRZvgT8DqaL%0AhaGXzHaMGHvi0d66P38RXfDc%2B" +
                    "H9l%2FjGtdS1zgiMJMpCUFtDc3OPgOuA93sReqBsq%0A7fv5a%2BLSdfFZUPgUkXM2ur0eA%2" +
                    "BniNE%2BG3mbDcr%2FcuILYI8xTs6xbHRIKVl2G09eS%0ABZMEyqH3duIAi0M42r4L%2FuvAB" +
                    "TcEyVKvY%2FQHFmFTj1tSzqSD5PDv%2BnN0ihNR16R9%0AN56PMcZazvTdChhXuA3MNciKoJt" +
                    "bZ785c%2FdwRL8bz8rr7Wj6iF%2B3Qm6kgbkef%2Fo4%0A6D8u8G1eDfSWuwtXVqIOuokd%2F" +
                    "mYgNIVZwt1sJukuGv3eL76b7Mhk3lCEjE8uSOf9%0AN9mbLErel5VUTzNTVpA336aBnMKjEsJ" +
                    "UIOg0sU0q8XAKeSjcrIuBrsaKpjq7WDXp%0AFA2eQkpHpwZnlWjVMOYRREdji3G%2Ft32ATTc" +
                    "hNXl9zhQsioqQbfUtWkj2WvltE5oz%0AO85ddVUniqpQPdQaojZ5%2BdPZ8SBC%2F4eUp3z4J" +
                    "4%2Fb0fWSTPl%2FtLblFy1HJs0lKG5Z%0A8AaoCGF5TLPoygXjBk0ImikeIGlYIShVOqG36RJ" +
                    "lMh4xOQCmY0g9nz9LdCEHJ%2BuC%0AkWh%2FoREBhSMnqlmn1ic%2FDG16h17E%2FtiOuOxsq" +
                    "TfIGlkLSShXDoiTjxgm527FA5HB%0AwUwDS1ov%2FOYtlQEBD%2F9f6jwJxYjdBo2pUy5c%2B" +
                    "gA47BtW%2Fzz12MKhRAHd%2B%2FbVbTv6%0A5JhlBw1Jow0ckjcbnDRqBP9EL%2BErAlc2UzG" +
                    "a%2B42Ahrc2HlDvyMJCcxLt0Fa2nhXG%0AYWGHsQbHxgbePWHozwun2RXaAvvBonhBaYtcn0Q" +
                    "PNEtArB9uyO4YqXXoH1%2Fl0%2Fgh%0AIAzuR%2BLNymwdOBXpyiVFMJb6xyQF40aT31kI8Ge" +
                    "%2BUkBbkWDphcEPogd59krBEpwz%0AfBfPdlGoTrSwfbKbshM0kiEbPh%2BESMVvypg%2BPZo" +
                    "1Qp0eXYt7gjlYYqNzQHWobTTr%0AIQjY3T8vml7XlcPzxLFqvQliuIZyRLczvm%2BwDhTj%2B" +
                    "J%2FdXAK0SHE%2F9XdqKY014j5t%0AWjUfy9iD6seZ85ntAWdxHmOkytANe3QfyVxbO3N31nF" +
                    "e4uqJmW0RaEDx0em3k9YM%0AYLe5OwK%2F49IpUj5gV1R3wnN0uNOZNOdhkyVJynLDJXV5DLo" +
                    "WO3yGMPM3iM%2BZGujk%0A4QrpXjVFscfTHy%2F5%2BbNFGHnapljzli9cbKqt3j610wLQa1p" +
                    "Hj6K3xJOANwr0Vdjy%0ABFGwpREQDPceSNREFA%2B7FdPh7WQe7P5NbfYuBXGZZeIvRZ6R0EH" +
                    "i8Agxn1426qYJ%0AEJNr%2BqO2r49EhfCdwbizRLhBsqMJQIirkf5sI4w5RIgpI9ggkv%2FgQ" +
                    "iqxvqFcDdK7%0AAVK%2BeZiB2bvY3SVaH49hWaCE1OZ28gDYPlce6ARxznq1eqQhvgUyOffjp" +
                    "DjPgSkF%0AQhQCj6%2Fle9lunPkNKEYUhFr2eBBabBejRAsdLTOslG3yltICpBjHGqOB2CaDl" +
                    "HgL%0Aa5eoeqHusBAx9fmYtd0Zi474cGay8RjGtq%2FE%2B8wDTsupnYGbsHF5pDXC7erW9gy" +
                    "Z%0AMzIE8wAZ%2BIxbhG7JXVtHaPWAbvl1ac7YBV7rpBYRKuvZvDQ9BL%2FtYy59HA%3D%3D%" +
                    "0A%3Dt1CY%0A-----END%20PGP%20MESSAGE-----&message_id=166b194b21a0997c&sen" +
                    "derEmail=human%40flowcrypt.com&is_outgoing=___cu_false___&account_email=f" +
                    "lowcrypt.compatibility%40gmail.com"
            ),

        "decrypt - [gpg] signed fully armored message" to MessageInfo(
            content = listOf(
                "this was encrypted with gpg",
                "gpg --sign --armor -r flowcrypt.compatibility@gmail.com ./text.txt"
            ),
            quoted = false,
            params = "?frameId=none&message=-----BEGIN%20PGP%20MESSAGE-----%0A%0AowGbwMvMwMVYf" +
                "y8j1GPd8g7GNXlJHCWpFSV6JRUlcSH3akoyMosVyhOLFVLzkosq%0AC0pSUxTKM0syFNIL0rm4gIS" +
                "Crm5xZnoekEosys0vUtAtUkjLyS8Hq9VLzs8tSCzJ%0ATMrMySypdEjPTczMAYkp6OnDrODqZDJmY" +
                "WDkYpAVU2QJVTh1Tmeb3HLhpxtYYQ5i%0AZQK5goGLUwAmYl8mwDC3yqJ3RqXeax2n108b42sc%2B" +
                "I29zE1fLvdgq1Tz3ZL0a2Z5%0AXSTDobXyoiGnj748k%2F8iX7dJYc5C%2BTTmPMXtPmYJKmd7V7v" +
                "2x6675BfR%2Bm25ednr%0APfEB9k%2B47iQ9yNsgu9TG8NC%2FhhccalMkT1UUcv7V07mW2ZRbfvS" +
                "op1ZSU%2FbXm3c%2F%0A8nd%2BZShfmrHQYMMfe3Xmildmbhs2f7S6I8G%2ByamhrH1XsnXKlc%2F" +
                "ca63S53TU7u5e%0A%2BX7vil97zTc3cDgtP%2Fuw6GB6mmTo8mqlb20GytG1LuYzZftP55XYL7XyO" +
                "5M8Rzx2%0AZcLBPTsfzs8o6bgxt0fBucIlds7nzLOyKld%2BG2u%2BuSqzuj9wgpeOSX149f%2B8y" +
                "7N%2F%0Ahl5nbXIo3qL3QXaWwsXvh7fITVp155%2FbxSXKX65fuLmh%2BET24Z9C7V8iGf9M7v76%" +
                "0AtI%2BjSNRu7cnAttxlX4tOGHhtuMH%2BTU8nNv1cPEc1X%2FH1VRv95mWabl3lP%2BHVmou%2F%" +
                "0ArkyN1%2FsWl7tS%2FfZP3vVlp3MSPvqy%2FP6T3VKhXSYdWFzhyblB6KhqzAbBuuVf%2F2bY%0A" +
                "KRx1239v9uZrM3yEZOc0JtzNz7Lh7xb6e89tIne4blx81aRT7b86YroUHGfe0PF4%0AsHjRnQWdme" +
                "U2kgcmH%2BLUEdxd4bJgx%2FSQwPrb%2B6zieQ0mLbDsvZm7gHFPeq5ZW%2B%2Fe%0ABU8%2FcNc2" +
                "bd49KWrdT8%2FzKpJ9KmvV9uz4AQA%3D%0A%3Dr8So%0A-----END%20PGP%20MESSAGE-----&ha" +
                "sPassword=___cu_false___&msgId=1707b9c96c5d7893&senderEmail=flowcrypt.compati" +
                "bility%40gmail.com&isOutgoing=___cu_true___&acctEmail=flowcrypt.compatibility" +
                "%40gmail.com"
        ),

        "decrypt - encrypted missing checksum" to MessageInfo(
            content = listOf("400 library systems in 177 countries worldwide"),
            params = "?account_email=flowcrypt.compatibility%40gmail.com&frame_id=frame_DfGthW" +
                "pEth&message=-----BEGIN%20PGP%20MESSAGE-----%0AVersion%3A%20FlowCrypt%205.0.4" +
                "%20Gmail%20Encryption%20flowcrypt.com%0AComment%3A%20Seamlessly%20send%2C%20r" +
                "eceive%20and%20search%20encrypted%20email%0A%0AwcFMA%2BADv%2F5v4RgKAQ%2F%2BK2" +
                "rrAqhjMe9FLCfklI9Y30Woktg0Q%2Fxe71EVw6WO%0AtVD%2FVK%2Bxv4CHzi%2BHojtE0U2F%2Bv" +
                "qoPSO0q5TN9giKPMTiK25PnCzfd7Q%2BzXiF%0Aj%2B5RSHTVJxC62qLHhtKsAQtC4asub8cQIFXb" +
                "Zz3Ns4%2B7jKtSWPcRqhKTurWv%0AXVH0YAFJDsFYo26r2V9c%2BIe0uoQPx8graEGpKO9GtoQjXM" +
                "KK32oApuBSSlmS%0AQ%2BnxyxMx1V%2BgxP4qgGBCxqkBFRYB%2FVe6ygNHL1KxxCVTEw9pgnxJsc" +
                "n89Iio%0AdO6qZ9EgIV0PVQN0Yw033MTgAhCHunlE%2FqXvDxib4tdihoNsLN0q5kdOeiMW%0A%2B" +
                "ntm3kphjMpQ6TMCUGtdS7UmvnadZ%2Bdh5s785M8S9oY64mQd6QuYA2iy1IQv%0Aq3zpW4%2Fba2g" +
                "qL36qCCw%2FOaruXpQ4NeBr3hMaJQjWgeSuMsQnNGYUn5Nn1%2B9X%0AwtlithO8eLi3M1dg19dpD" +
                "ky8CacWfGgHD7SNsZ2zqFqyd1qtdFcit5ynQUHS%0AIiJKeUknGv1dQAnPPJ1FdXyyqC%2FVDBZG6" +
                "CNdnxjonmQDRh1YlqNwSnmrR%2FSy%0AX7n%2BnGra%2B%2F0EHJW6ohaSdep2jAwJDelq%2FDI1l" +
                "qiN16ZXJ2%2FWH6pItA9tmkLU%0A61QUz6qwPAnd0t6iy%2FYkOi2%2Fs1%2BdwC0DwOcZoUPF8bT" +
                "BwUwDS1ov%2FOYtlQEB%0AD%2F46rCPRZrX34ipseTkZxtw3YPhbNkNHo95Mzh9lpeaaZIqtUg2yi" +
                "FUnhwLi%0AtYwyBCkXCb92l1GXXxGSmvSLDSKfQfIpZ0rV5j50MYKIpjSeJZyH%2F3qP%2BJXv%0A" +
                "Z47GsTp0z5%2FoNau5XQwuhLhUtRoZd1WS9ahSJ1akiKeYJroLbTg10fjL25yp%0AiaoV16SqKA1H" +
                "%2FJOuj6lT5z1nuez35JjeSpUc7ksdot60ZovMfWC%2BOGRnkYKb%0A7KxFd7uaxL6uOBOFyvRxYe" +
                "ohKd73aVkiKpcWd4orI18FhlftFNAwIdsmfzNc%0AmzTHZaUl89iYxEKR6ae6AKws1wzLq0noarsf" +
                "2eKBVbTSfmK3S3xFqduKINnc%0Ae5Yb3F5adSj1dUjm1BZ4aqzsgKyBb%2BJ8keG9ESsnFOyxOIUX" +
                "DM1nIo1IOgzC%0AM928Jb9GVa%2BuhdXRrb5cLjTihTusJN0I8oJrwKkwIpCJVgPMdDLkeubrMBQ4" +
                "%0Afbpl4V76sOU2Nx%2B6nG2FnFBFBFohOL%2B0nTK5%2F6Ns9ateN7K9VP%2B%2BQcoeqfPk%0AI" +
                "UO3%2BlCZW%2BtrTSvvFId3ziUVsPTeuAS%2B7nxSMfWZ%2FK9Ci6QV%2FXnx3F%2FqSmuS%0AAUm" +
                "4zPQ1EjZf1N%2F5K%2BvhcCTN4MMx406VlqtedkXL2KPwZ6jDS%2Fww8RfcmPnD%0As94ct0WCZZt" +
                "NlnQq%2B5h0ybwTJNLC2QFyrhhPqztVY95n9La2Mw5WITCWzg%2Fd%0AIBUceW%2FOwHYtePyaSQk" +
                "CnegDw%2F2mN2%2FGC8d0OlwULcTYG6uVenGv2UOUbCr3%0APfy%2FEb%2FVqUEZK00PdvVQV7FWY" +
                "AshuTFPTqidph04CgQvBpi3SDEEo8SkEIFS%0A%2FiEeRQaWjFEXKUI3FwKXPJQWvFpbrXBOAjnxX" +
                "XbAFYOLxdydmq1GVl9Mm3GU%0AClc9g6t9vaYDBPx2gN562%2FCM%2FnT8Vq45VHe79XkrrcHDwLn" +
                "7yeHJScNFsib%2B%0AVvwTPoUftlhC%2Fai21D403TsJpm7ZmPcDjagoIcXrS%2FlN03z79RBmSKF" +
                "tYiXW%0A4obkKSGow61vMBh2%2FXLVYKJKpYKm%2FGnVlJxA0zQVl558x8I%2FnAMaxSzwx%2BZY%" +
                "0AwaVU%2Fs5PLZ7Ghg3MOguiRTlflKUQyL0A7NR46OjFgUnHAZRxr4KO3GoxVPy4%0AXLeS4%2BWl" +
                "68s7QlV6WF1IKCHWEUMEeRRea2%2FOvvlS%2FoLs2MNNWDemlJ4SiXHf%0AxINU38Txo84A00NALb" +
                "KppsSyy9Gwj%2F%2FrO%2FFcerupkfeuOm9nHFwIQeeC5bWD%0AmmRlC90r2jY8gM%2Fv3Jjy9h8P" +
                "bXWxh9MUpc7%2FkAcTwdGlMxiVjE29p065qTRr%0AOi6sJ7pWuYTfWldZqTVmaBjlv0zuXQ8Eo8o%" +
                "2FUSvoTs%2BoihYIMcqReqdeqr%2FN%0Ae%2BsDtYKRg%2FLKp%2FJJ5nAQzVMP67DxkgwLNxx0ij" +
                "BLysaQmvRlsiYWayxZB1Xd%0ABxA2bjZRvsmww%2BhgSKNlcsiubJGBqfqvgmlebZuJHHSC1L6mdM" +
                "YgcihKmYAj%0Ap%2BHFLyqgyeRVMdjRHcrEdxNPG4fJmlk1bYiVQQ4XAd72w%2BAHS%2FseZ5HzbA" +
                "K0%0AomuHYUD5PTEqZ1K9JObSsh3XMUkJK%2Bz3BnrOxnTOOyG2r%2B4FxizH6rfz%2FPgg%0AsPx" +
                "qxE9ELUlgQe8plcPFge6aN9tUoSe%2BvMtDaEAqKw9JwofBF7jlxTqMMvQC%0AgWbn9x3W5o4Vrnp" +
                "jYGtPl8sh1QREu0A%2B0PUJAKL4A3GSMYRouGewLSMNJlOg%0A%2F0pPF6qB%2BFi4GJ7ju5C07tf" +
                "r9z9UqRj09kDXJuoJd95NdSiCz6ndugn6gs8B%0AQf%2FXPxZVefeMLiB6p8pG0iZ%2FjcJjyYJLt" +
                "Tg6kA%2B1%2FffmJPfH%2F76ZA9dgEJLj%0A%2FW2u0Lp4NY8cwqcXuGKgl72TVJ34Iawl35Y0yr4" +
                "7k%2F7Y1vEQ5Q3bT7HP5A%3D%3D%0A-----END%20PGP%20MESSAGE-----&message_id=15f7ff" +
                "bebc6ba296&senderEmail=flowcrypt.compatibility%40gmail.com&is_outgoing=___cu_" +
                "true___"
        ),

        "decrypt - [enigmail] encrypted iso-2022-jp pgp/mime" to MessageInfo(
            content = listOf("=E3=82=BE=E3=81=97=E9=80=B8=E7=8F=BE"), // "ゾし逸現飲"
            charset = "ISO-2022-JP",
            params = "?frameId=none&message=-----BEGIN%20PGP%20MESSAGE-----%0A%0AhQIMA0taL%2Fz" +
                "mLZUBAQ%2F%2BJgpmkgscJEB9uAiFT6TbeZsBqD%2FrRDInab9OevrRBWOZ%0AwQ7UxS4OkM8M1jo" +
                "flEgtP1ZjNkCCuOG5RXR3JZFkUeQbvtMc4mpx%2BOOjYbwHwNNE%0A05wbcIHn380axNPWMYqe8%2" +
                "FiCK9wFWhMDwtpDfJ0PzLKAhnjFhymMWjmXB2avejaS%0AiaKQRelmUiNt5Tk6FAu8UnOAbr7%2Bu" +
                "LvFBzzjyELL3pGzDBLkawhUT2QZ2nqrC1Ns%0AJ8HEXnl0TBI5T9rlK5i6YQi2i26SWk8QkM0ov5O" +
                "WVK1qISf15VHeP5uLXch3MfHu%0AEfQEubo378Jbka9QMo1%2FE%2F8ublGQReMpsvbrWto9HqfPS" +
                "XUGe3hUQcIRi3nKuQBx%0AnbMWonnNE7UEhBFytLL2w%2BMmBDlkePa7zDngOjQwLpYNVvrxJnCjk" +
                "6Skcrn%2Bx20D%0AYkGziEubqhquMRLxJht4UTLuRSLI8j1NtIRZ5Q9Bi%2B1krSJz527cbq%2FIF" +
                "BU%2BEmNV%0AfCcR1nYbF5%2FhyTwB7aZQyxCVlRWKlYfwv4%2B7q9cj5wCBuLCY7ZKucEbzodehR" +
                "cEt%0A4C8Txg2KkD7%2F8%2BTt60KcqjvyDtQkQYNSaubugsG2BAmJpYRU6KFVGDlpNe6gGEQI%0A" +
                "hnVQq5UaZ9z0DcevE25Xr7fg2mLN7yHRRSauvOGlMnP98d3gDluQlbvAzk5qOMqF%0AAgwDqn0MG9" +
                "%2BUHywBD%2FsHF58ogxK3kAbITjA453U15KkR4bAqcH343mPfjPOPTyAb%0A3IMoYbQV9SbHupta" +
                "v6t9rhtrGEkNVunLQLrGYNbwrQx253yqgN%2BdRYD8mn101yJM%0AFcN3R6PDCxAL4hW0cXjSspaq" +
                "e1mx8U7pz%2BLn1DrC4X8O9HHgMrPvUl18Uc14fAkw%0AZm%2Bwk5vzVYxHp8WsQXb9xpe1imlew7" +
                "jPuHZkNSA4k6YDoGn%2FwpN3mEOKE3BYq6Ro%0AhnTaapIe%2BJIzsa%2FH0HXKcD0ztFeRUEyyjd" +
                "%2BdE3vdJYehZrEQIjsM0ocqbn5tcf1W%0A9DP0OXGylTfNbBMT6PQ4N5gfyQDext9Z3QOT0c3Hcm" +
                "UYHJd865jR5nXHGzsGW%2BUr%0Ad0Z6AaCsSCP8WPUNixLzgCdB7EQ6Z5PB4etj9%2BFmKYvEbFia" +
                "Or9hrY48ny%2BiOJjq%0As0dudhgZkE8XpA9jcJaGnM4UZdFnssiTydlqaFWYwjVk8d4CsjrsTx%2" +
                "BJNRWOSVHy%0A9WBbUFQc1eH01rv1sfL467Eyzhh92SCfHooHN9lLtF2mDh43ZQu0ReW8aBd7RzHc" +
                "%0AKJC8E0wcslzLqfF%2Bx7rh0Vt54Y3i0PS9H9RDWATssCx0V3ySwbnxqme6zIa5qKUN%0AkbSqk" +
                "ucmzZKnaksb46S0zJyOB%2BQV%2F8ntYErmLsX1pGFvPFBm0%2BGQ%2FyQgpUiJhtLp%0AAW8jKCF" +
                "MyQBHhBKyG0k8Dn9f5mO8rE0xG982m%2BnGwlMKJunn%2Biiyz561V%2F2ebb5e%0ARPCj12RUAIz" +
                "KicPqRaPCaXhEyD30y1rDCHO7vpCB1CgnbVfRcPvTOOuUlGlrMYWa%0AZlAyrc5RkAiSCLUJab9ZT" +
                "f%2F%2FT34dmP1p0bmIN3Mnwu3XsbEdZnoQxqPSl0UyfqiL%0A4e5uGe2Za%2FrxykM9CuG0f8vtF" +
                "WsoNhJkTugdZjfKUZdnyfdsmZhNbKlJcuB7prvb%0A0Gl0%2F3fNns6qv%2B%2BR%2FEGNHbZhSxw" +
                "%2FqZXGBwGa0Y7hwwsA1Q6ObXgnZA1TDqFUhFk6%0A%2FcDa8FlRD1jj9rKyeuwwLryRy%2FLhoq1" +
                "LL%2FWV%2BOiUB%2F%2FSldGaHkqXv9%2BCJNhmNwEU%0AiC5mZYyhGGbsVcBxuQigilyMpDQJJfc" +
                "UiqfN8KL%2BN8ICpnuPGgaMQ97SLeHq2Mmm%0AehcEZwVQZGlCnQJNKmhbqqxJB7WmdBRKTDiBxE5" +
                "qz5r3grB%2F5v%2BMbyM6G6MyAnkP%0AA%2FUKX0QUZsPDR41XVWhZPTDo%2F%2FZ6aIKJwlgB3E9" +
                "vak2JkD4%2FpdgzyAM28HOTUyJ%2F%0ASfBVLd%2B%2FjxHIVlm1IaLUAzvJjG0NFlXvD7Pkzs5pX" +
                "mUf%2F%2FbTdFXNA3uh7VBSGQMl%0AkaeyuemQwiW2Ray4tYbUq%2FFCzl%2B8862JBY98w38natr" +
                "A%2B%2BWMLHIox0rhMIG%2FvoyK%0AU1%2B2KKgED414MsC309jn%2BP6WCZGKt34BXSfDp%2FRbg" +
                "wP3X0QIxSYOxtmX8fjKlVPR%0A9FPmkwFvIYsE14MSB16y2vxSEt8JFKoGhXRuVxlGoYuuZrpERfh" +
                "ynnkwSLkw0zln%0AMNUUihw9AePivi6H0qy%2B7DpUy%2B41CW8nxkx%2FdePcdbAq84Y71FyfM7g" +
                "bLu04EPZ0%0AhTzzgSLDSWvLc7vWRGbqI1erQhfadQwiUzMd0YAyImWmnm003dxfRNNC0oCYDE8V%" +
                "0A3QGFqOmr4AqcbMGIWBGiP0LajmehJEv%2B8GkIkuDwQRtkgaAkHwDMigujFtraqGEn%0AmduYmY" +
                "BW88YDsXD9Jv24d4Pt2Ce7P4lc4DEAU3vqUMZFdIwHanjKSNr8O6aXXd0e%0AwrFjc71tUTNGF0su" +
                "Ni%2B74ol0rlS1seQNiijulEW53ngK8z5brNSd1N56H%2FwcYx81%0AqSyeqnGYHphNbpwhBTqHkU" +
                "RBlwygxI2%2BCuMSR96j49ko9AZZHYl6DBAqHLKYWHGi%0AvqvpG%2F%2B%2BFFy0AMSxjs%2F%2F" +
                "Ce5lTS3y3skfneaZ9sgH7o%2BUXCJ7Op%2BmtLdHGjn6%0A%3DGAOS%0A-----END%20PGP%20MES" +
                "SAGE-----&msgId=16f66f1da9d50d05&senderEmail=michael%20<censored%40email.com>" +
                "&isOutgoing=___cu_false___&acctEmail=flowcrypt.compatibility%40gmail.com"
        ),

        "decrypt - [enigmail] encrypted iso-2022-jp, plain text" to MessageInfo(
            //content = listOf("ゾし逸現飲"),
            content = listOf(
                decodeString("=E3=82=BE=E3=81=97=E9=80=B8=E7=8F=BE=E9=A3=B2", "UTF-8")
            ),
            charset = "ISO-2022-JP",
            params = "?frameId=none&message=-----BEGIN%20PGP%20MESSAGE-----%0A%0AhQIMA0taL%2Fz" +
                "mLZUBAQ%2F%2FembFGVPRuVfiUujhLesQa6a4sbp%2FPOQcAsy%2B%2BO6tD%2FVA%0AwrQtPhJen" +
                "iYVFeOs%2B37MWy1PkOUn6AAvgasHtlMCVnthxavG1onImCJWyC0NdgYn%0AhrIN9aPmOY7UGhVzp" +
                "U%2FGTxE1WHJHGMMGShmKbt%2BJThtAvmufuDK1DSho3kcjGEs9%0AwpY0DU0%2B9I7xEmobgQqK4" +
                "jyzLBLNx4aHl2qurKSmjghmk1ZMW4oluckrDmmQ3AWT%0AZ%2Fq7bnbP1GDNJV8cxR7ed4k6HCzkr" +
                "X%2BBxL308E8soLtg87occ18QoJAIRAHU0kx5%0AJlS9%2Bfh%2FjNwKanZJjCWv6hqZKz9iUocRZ" +
                "D9iPqh9dhjsKalqkRaxuPM2eJkZY%2B91%0AjG8tsHYTLeY33A4aUpdA6FpNR8Uyz8Agv%2Bx8%2F" +
                "aFp8GxSNIuUumf6bSIk2Oudt%2Fa6%0ArWvZO%2BM%2BUK53a4k4ibxrkv4zsE8CbijjCP8BvUrA3" +
                "7023GEWkOHIyMoFFy0o06W1%0A56wTP2bLmKbujeES%2Bdkzjrr1r9X6oDBwpoPABKSAjIKFQKcxW" +
                "vhMgz4WO3w61g3F%0AE8U0Rlx4lB4Ce1I0qzu8S4hkaZ7sYcKJ%2F211pzsaf0BfxZQdrfyu5kse2" +
                "75YgTUA%0AbObnoW2sAWg8fX9JwuL9JVArnJ%2B6AOQjvNG9fr%2FuM4thV%2FzwqBUWfQ0sasDjj" +
                "xSF%0AAgwDqn0MG9%2BUHywBD%2F9bMrHNk%2FqirxpfIRa9vZcZssXv7A61XUZy2IVum9%2Bp9c4" +
                "W%0Aswd23kQOfC%2F82Fx75CwMQ%2BzzdP7%2B5tqeNfm3%2F4vfObLCmszf1%2B%2Bj3nVxEEX8s" +
                "WpC%0AmgHobD3uZPwgShvgcy6ZHkfz%2BBrxqqTJIZ6xD03VgzmNg2cuAHD1YVUKbTHGYcKM%0ACY" +
                "0b%2B1VG6lv4f78xiB0v8aw%2FaPTvtx0rY2g0YZHaE0JXT59cMNTMORNiE8h8guLB%0Alf6hcwct" +
                "RN%2BsJw5oW%2FsaXpgFJSzVbQrwp0a1b6Ftzqv%2BqyJL2%2Byay83RaPX%2BR7LR%0AJy9jPrwB" +
                "bzwCVbJBBSfeQ0zXkeNAOso83rE13UjxPsl%2BkU0ajxy55K%2FP%2FcLO6KKs%0AKtFN7UGo2jGe" +
                "lpqDoGU5FwOoGeEaYW%2BInrZryyV%2FA2bjw6Zmfbh0GMzls25fK%2F9O%0AOJp%2FD0yqEmnkU6" +
                "0O6eDwwwxY7VNqmtuOTZ4z8PIaV9LWuftVOeOG99%2B9g6280CKF%0AYYHAxgb559v70V50bk%2BZ" +
                "91rdA1SnxSq9wOkUu2K1BmkTdqEO5jxWf04MGrvROZUA%0AdIKQ%2BPYibnRo%2BSObBUn4Otlfhe" +
                "l1tJ9wWWjJLpGJ1Zm3FaoCVH%2FMnnvhF48Q5JNR%0ASDnqTg4wWd51Tokcnz2PoPrxRN3jacI4d0" +
                "GZiAtsmB28mcKjdB5UYoXEy2MazNLA%0AyAHzCHRtOJ7eOcStwltwnbh67%2FRCK9OCegaiSOMAcs" +
                "EciVXUpT%2BhVl8oMl6IvJDk%0AFq1CwOL8t3Oj3W2igPkm2EejHl1dkz2JXPHjfHqt24tTtWRa3x" +
                "uotoSvMAy%2BtfKT%0ACwg67nQan%2F13hl3eF0XXLCD%2B%2BaotGSahUePsgZU79oedY2vmcofU" +
                "f743sZ%2FN6aMP%0AgELzwyLm7LzcFLjeokhNUDYpBgrH5%2BFcFZqpiTQhILONSvenncP4k3FDjC" +
                "87DG5J%0AyIckBN1KeU219vaYHEkmSmU3egfEYRMw2HznFAaiMEAnDoGs0ZTqNOx75ktZLpfS%0A7" +
                "79APSTDmS%2FhsXXo7D8%2FmyYWO5RMxFzGL7SIcXkosqa%2BTS3FJ5198epH0xLNrDhM%0A1lMO2" +
                "ZU5qb2TNA%2BWvSviiwWsZ%2Byj6kD1rzrvEg%2B%2Bq1b67s3oogP08wwHcfX%2BUiND%0AvdGeV" +
                "d2YFPX0kszhtfJDEYAkJ8ERe5RKQqeNXdk8XMYq2irp3AVBTBDkgTgMDPSU%0ACI3g89S3eldT%0A" +
                "%3DP4O8%0A-----END%20PGP%20MESSAGE-----&msgId=16f431a0b9056562&senderEmail=mi" +
                "chael.flowcrypt2%40gmail.com&isOutgoing=___cu_false___&acctEmail=flowcrypt.co" +
                "mpatibility%40gmail.com"
        )
    )

    private const val messageParam = "message="

    private fun decodeString(s: String, charset: String): String {
      val bytes = s.substring(1).split('=').map { Integer.parseInt(it, 16).toByte() }.toByteArray()
      return String(bytes, Charset.forName(charset))
    }
  }

  @Test // ok
  fun multipleDecryptionTest() {
    val keys = listOf(
        "decrypt - without a subject",
        "decrypt - [enigmail] encrypted iso-2022-jp pgp/mime",
        "decrypt - [enigmail] encrypted iso-2022-jp, plain text",
        "decrypt - [gpg] signed fully armored message"
    )
    for (key in keys) {
      println("Decrypt: '$key'")
      val messageInfo = messages[key]!!
      val r = processMessage(messageInfo)
      assertTrue("Message not returned", r.content != null)
      checkContent(
          expected = messageInfo.content,
          actual = r.content!!.toByteArray(),
          charset = messageInfo.charset
      )
    }
  }

  @Test // ok
  fun decryptionTest1() {
    val r = processMessage(messages["decrypt - [security] mdc - missing - error"]!!)
    assertTrue("Message is returned when should not", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue("Missing MDC not detected", r.error!!.type == PgpMsg.DecryptionErrorType.NO_MDC)
  }

  @Test // ok
  fun decryptionTest2() {
    val r = processMessage(messages["decrypt - [security] mdc - modification detected - error"]!!)
    assertTrue("Message is returned when should not", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue("Bad MDC not detected", r.error!!.type == PgpMsg.DecryptionErrorType.BAD_MDC)
  }

  // TODO: Should there be any error?
  @Test
  fun decryptionTest3() {
    val r = processMessage(messages["decrypt - [everdesk] message encrypted for sub but claims " +
        "encryptedFor:primary,sub"]!!)

    assertTrue("Message not returned", r.content != null)
    assertTrue("Error returned", r.error == null)
  }

  // TODO: missing checksum not detected (is it same as MDC?), and should there be an error at all?
  @Test
  fun decryptionTest4() {
    val r = processMessage(messages["decrypt - encrypted missing checksum"]!!)
    assertTrue("Message not returned", r.content != null)
    assertTrue("Error returned", r.error == null)
  }

  // -------------------------------------------------------------------------------------------

  @Test // use this for debugging
  fun singleDecryptionTest() {
    val key = "decrypt - [enigmail] encrypted iso-2022-jp, plain text"
    val messageInfo = messages[key]!!
    val r = processMessage(messageInfo)
    assertTrue("Message not returned", r.content != null)
    checkContent(
        expected = messageInfo.content,
        actual = r.content!!.toByteArray(),
        charset = messageInfo.charset
    )
  }

  private fun processMessage(messageInfo: MessageInfo, keyIndex: Int = 0): PgpMsg.DecryptionResult {
    val urlEncodedText = messageInfo.params.split('&').filter { it.startsWith(messageParam) }
        .map { it.substring(messageParam.length) }.first()
    val text = URLDecoder.decode(urlEncodedText, "UTF-8")
    val keyInfo = privateKeys[keyIndex]
    val result = PgpMsg.decrypt(
        text.toByteArray(), keyInfo.keyRing, Passphrase(keyInfo.passphrase), null
    )
    if (result.content != null) {
      val s = String(result.content!!.toByteArray(), Charset.forName(messageInfo.charset))
      println("=========\n$s\n=========")
    }
    if (result.error != null && result.error!!.cause != null) {
      println("CAUSE:")
      result.error!!.cause!!.printStackTrace(System.out)
    }
    return result
  }

  private fun checkContent(expected: List<String>, actual: ByteArray, charset: String) {
    val z = String(actual, Charset.forName(charset))
    for (s in expected) {
      assertTrue("Text '$s' not found", z.indexOf(s) != -1)
    }
  }
}
