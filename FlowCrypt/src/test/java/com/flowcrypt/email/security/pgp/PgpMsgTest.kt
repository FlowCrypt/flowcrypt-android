/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *     Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import org.junit.Assert
import org.junit.Test
import org.junit.Assert.assertEquals
import java.net.URLDecoder

class PgpMsgTest {

  private data class MessageInfo(
      val content: List<String>,
      val quoted: Boolean? = null,
      val params: String,
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
            params = "message=-----BEGIN%20PGP%20MESSAGE-----%0AVersion%3A%20FlowCrypt%205.5.9%20Gma" +
                "il%20Encryption%20flowcrypt.com%0AComment%3A%20Seamlessly%20send%2C%20receive%20and" +
                "%20search%20encrypted%20email%0A%0AwcFMA0taL%2FzmLZUBAQ%2F7Bwida5vvhXv5Zi%2BqJbG%2F" +
                "QPst11jWfljDQlw1VLzF%0Aou8ofoIEHpvoFgXegZUnoQXBmlHGD%2BXLs9jG%2FTV1mtE2RWq4hDtqiTQ6" +
                "rEIa%0AbrN3Nx77Yr%2B4EN1aKI20aTLEPTIjVU2GH2i9DAmjHteBU3nkL9Z3yecB8Pn8%0AEdhpCRY6cj2" +
                "yrhJ5MPwmXrus9OFv39wA2DqYpqW5Be%2BKD8mipZ2CtJo5xtin%0AaeEhpWSDsdg26rjx1nz4dA0NcFzZK" +
                "2p%2FBPfPIFzRvmoXoWFigpUnwryEoCqX%0A%2Ftgmcrv7PqiYT5oziPmMuBc1lb7icI%2FAq69uXz2z6%2" +
                "B4MJHOlcTEFygV36J%2B1%0A1opcjoX%2BJKJNn1nvHovBxuemcMwriJdmDj4Hmfo4zkd6ryUtGVrMVn8Db" +
                "Rp6%0ATWB%2F0MSE8cmfuiA5DgzdGbrevdL6RxnQDmalTHJ5oxurFQVoLwpmbgd36C4Q%0AxMfG1xEqFn5z" +
                "vrCTGHg2OfS2cynal8CQDG0ZQCoWwdb0kT5D6bx7QKcuyy1%2F%0A1TXKnp1NamD5Uhu1%2BXuxD7EbvDYU" +
                "WYh3bkqgslsoX%2BOUl%2BONdtMD5PswArd5%0AKisD9UJuddJShL4clBUPoXeNrRxrU6HqjP5T4fapK684" +
                "MeizicHIRpAww7fu%0AZ8YtaySZ%2FhoOAKWsx0rV4grgJV7pryj4ARBRa1pLL9rBwUwDS1ov%2FOYtlQEB" +
                "%0AD%2F47fyD%2F6BvepqWmZXj7VLl2y63eE0b%2F6hf5K%2BIzv5A%2F%2B5l%2FEnjFx0rq%2BqeX%0A6" +
                "hftYZBUAbbBvKfxq9D5xsWg3tnhFv2sYIE3YpkCSzZpWJmahHwQOVNT0ASw%0AgbO25OiTPlYPqfSkGYe0p" +
                "albL%2B4T5dLOwVilmrZ2bQf%2FrLePwA4RQpWDPYio%0ANDU0Xfi7TQcHQrZTpwFbVzNPXgCHnQkqF%2Bs" +
                "0v8RDJHnt9vVs2KEpi49V%2FYgN%0A%2BgZnZOeADL0rbre%2FPrIck1YSjZLbrWtQVk4%2BsCf0TjvixJ7" +
                "MNjA4NgdZPo0M%0AHke%2F9XBFie3NiZaW%2FcEIVZ7WnjB3IbhkmOMJd4LgdHKgmswJwCYm%2BXvpOI19%" +
                "0AFzU1vzZmfOA1nEJSuuCDNVUoKYIQA5UEYJrVJeGnVN5sU5jkdlX9xPtYceww%0AYFmLisuf9Ev0HC7v27" +
                "KwYQRDPNYRA8GeK%2FjY6aZdg%2BVccsnzEigdYL5Tm4JI%0AZrxp%2FG807bZvt0yZwWh0gpWOFgbVgrm4" +
                "Hpji5ilDyulZSW%2B8nJxB5tDoPzL4%0Aj4w9malje0c60GWNtiyCPLURyN63C2q144UpQjSU5r66oP1yF2" +
                "A97aXKbf4p%0AqO7cSNWEOTpqJkJrNFVKQdWvXZ%2BmvW1PQFmkkwish2HiQIXmWb04uV1pI8hR%0A6YWk2" +
                "ox9aZiJ664MpncgyJ5uIMlzVfYrX%2BAZRtBW36RgCTprIv6l1M5NcHMy%0AzEscTaSY%2Fe%2BpM5HzQKS" +
                "zX%2BzHLa5kk5L7veX%2B1G33saiqSJ%2FfK13%2Bk7qDNZQD%0Anbtaebfh2JS0Pdbub6FUFjPHR5PydU9" +
                "ltuppGEeYrOe1SxwiZ6BZfIXO2%2F8M%0AhA%3D%3D%0A%3DB%2FNE%0A-----END%20PGP%20MESSAGE--" +
                "---&message_id=166b194b21a0997c&senderEmail=human%40flowcrypt.com&is_outgoing=___cu" +
                "_false___&account_email=flowcrypt.compatibility%40gmail.com"
        ),

        "decrypt - [security] mdc - modification detected - error" to MessageInfo(
            content = listOf(
                "Security threat - opening this message is dangerous because it was modified" +
                    " in transit."
            ),
            params = "message=-----BEGIN%20PGP%20MESSAGE-----%0AVersion%3A%20FlowCrypt%205.5.9%20Gma" +
                "il%20Encryption%20flowcrypt.com%0AComment%3A%20Seamlessly%20send%2C%20receive%20and" +
                "%20search%20encrypted%20email%0A%0AwcFMA0taL%2FzmLZUBAQ%2F%2BKvSED2vb9fJMQd6lRTh0id" +
                "C7srhg4ESSf4ggCXFE%0AdeOq2IkV5dNhgWGGawFVVUTewMh3L3JklDoONlatBthc2OGNu%2BFyu5No7hhG" +
                "%0A3Jq1GkNwCqex0%2BG%2BGVhlZfN2LOAx855H9m%2FAGxYo6KLU%2BROmPZV8PZo5YJPr%0Ar8TrhhfHF" +
                "%2FPG4ZmQIcuvPI1e0ivgF74wP4cG0qaPEacvSxQ1ZuDwzdqC1kGv%0AseOTJEhpBG%2FD8YfbzUXVrX4Gi" +
                "OzIu2OhnlKfU6c0BJCTz%2BqmQRqYOZXLvKgd%0AnU0RzfLgMsd7Sy1lCpld1syY3bT4l0FIRWUtVx1NrJ7" +
                "cluicEPDiqJsEZntS%0AYy1ViiRZlnk2Xvx1Qpsh7fifUS8e9gfwPevYFhZ%2Fb6SeqpRFRDFGa0uP9L5C%" +
                "0A%2FCcWqiUaLUL8nF51CYzfIMeIEGBk0TiVUAn19mkQTFbtbIB9K3uQHjFzgnrL%0AnLaJ08Eme5NugtJM" +
                "UIW7bgo4CAddRjj0isFsoesUv75%2FmEsHJ7JRPICnWx4b%0ALPKOyP0anN6TYDgTC6IqvMOoNi0ZPEIpmG" +
                "mf7ZOWjR4eUT%2B9uBmBHEPwGbLQ%0A85Mcjy1C7X%2B0uUkIPsqXgF7Ya%2FpwTuZ8mDtF%2FFU3kR87y3" +
                "jlDZ%2B3ltq%2BY%2B5A%0ABJyMGXGf24%2BSquE1Q%2BONIzBwBqwXuYvRJwqA9vOtZ1PBwUwDS1ov%2FO" +
                "YtlQEB%0AD%2F0R6LMWFQHZQCIFkvXcB5r4X3J68tcLffAIVs%2BJnoyR6JECUuCZJdKLc4Aa%0AF%2BA15" +
                "GKiOnf5Z8RIg3Fn3nXuyN5rlWOu0yOO%2FXrnCSMHiYErTLUO6%2B6V6%2Bby%0Ai%2BPOAtAWptnJ7rGSA" +
                "y17ZgIYD9WNPdX8Bv1fWEOJII2rj%2B5CVyBsOZWrnlnP%0AHjHOQ6gHop7bnQlrpmpA95PLhyoW1LkEIoC" +
                "0jgrGF%2B0QXRqEfdwpQBCklZyL%0A%2FWAsG2GJLrHUgQALgpTys6%2F5P7VP%2BVSOaEnOJJExIZPkRVR" +
                "FzWlYq1avgJWw%0AEFGmKeg335%2FiThKBFQ8JsH9U22G5DD1BcfX%2Bqtm4n640zC5pHRpLJO6ggiCJ%0A" +
                "ZA1SCtq6TBSF1FTa158ZNgjkiGZfS%2BoZvrMW%2BS1691vMmJrwqiRlPg9PXCA%2B%0AouGrU%2F1FVyRK" +
                "Gx1%2FUki%2Fh9SaxDX%2F3uHOOwJzytNxGMJP%2F4Y1Y6hbDwDzcrCM%0AFlFHXiNbfB3uxiHD9wWHE44z" +
                "91MkqOb7%2FajoLXA8J8U3KJGFa%2B8JkZleRVnq%0Ar%2FUT8ppv0%2FozWzV59mTulYzRdIPSy6r4V0bH" +
                "16XGwZtHVrljOi4TrkExB9cS%0ATdcX96RMMYpJ7p7dGcxoHaRBY120BD%2BsJ51jGi%2FYupoZBdbg7KcO" +
                "AEelD2%2FF%0ALM1LzR9f3HUaYyKvdPL%2BC0OwINKCAZBPShfECZOiqrNWgHLWddAdXqexZFLH%0A0y7td" +
                "11E7UNcCZegIlwOYksW7yuuCZ2ZLLnfx%2Fu1G18nKBCealqNkaow%2FPj7%0A4q%2B0UYxfZnAl%2FrFuT" +
                "K9ndd8tWMSm%2F6xzWEbqe%2F8NKJrCwk%2Fnu%2BpvF%2BMuRvf5%0A9DuzZFiNRQSjSxSYvkyLuw%3D%3" +
                "D%0A%3DvxOj%0A-----END%20PGP%20MESSAGE-----&message_id=166b194b21a0997c&senderEmail" +
                "=human%40flowcrypt.com&is_outgoing=___cu_false___&account_email=flowcrypt.compatibi" +
                "lity%40gmail.com"
        ),

        "decrypt - [everdesk] message encrypted for sub but claims encryptedFor:primary,sub" to
        MessageInfo(
            content = listOf("this is a sample for FlowCrypt compatibility"),
            params = "message=-----BEGIN%20PGP%20MESSAGE-----%0A%0AwcFMA62sJ5yVCTIHAQ%2F8CkcWeLmCy8l" +
                "vANll0KbA9ymThNOmZjblBNRZvgT8DqaL%0AhaGXzHaMGHvi0d66P38RXfDc%2BH9l%2FjGtdS1zgiMJMpC" +
                "UFtDc3OPgOuA93sReqBsq%0A7fv5a%2BLSdfFZUPgUkXM2ur0eA%2BniNE%2BG3mbDcr%2FcuILYI8xTs6x" +
                "bHRIKVl2G09eS%0ABZMEyqH3duIAi0M42r4L%2FuvABTcEyVKvY%2FQHFmFTj1tSzqSD5PDv%2BnN0ihNR1" +
                "6R9%0AN56PMcZazvTdChhXuA3MNciKoJtbZ785c%2FdwRL8bz8rr7Wj6iF%2B3Qm6kgbkef%2Fo4%0A6D8u" +
                "8G1eDfSWuwtXVqIOuokd%2FmYgNIVZwt1sJukuGv3eL76b7Mhk3lCEjE8uSOf9%0AN9mbLErel5VUTzNTVp" +
                "A336aBnMKjEsJUIOg0sU0q8XAKeSjcrIuBrsaKpjq7WDXp%0AFA2eQkpHpwZnlWjVMOYRREdji3G%2Ft32A" +
                "TTchNXl9zhQsioqQbfUtWkj2WvltE5oz%0AO85ddVUniqpQPdQaojZ5%2BdPZ8SBC%2F4eUp3z4J4%2Fb0f" +
                "WSTPl%2FtLblFy1HJs0lKG5Z%0A8AaoCGF5TLPoygXjBk0ImikeIGlYIShVOqG36RJlMh4xOQCmY0g9nz9L" +
                "dCEHJ%2BuC%0AkWh%2FoREBhSMnqlmn1ic%2FDG16h17E%2FtiOuOxsqTfIGlkLSShXDoiTjxgm527FA5HB" +
                "%0AwUwDS1ov%2FOYtlQEBD%2F9f6jwJxYjdBo2pUy5c%2BgA47BtW%2Fzz12MKhRAHd%2B%2FbVbTv6%0A5" +
                "JhlBw1Jow0ckjcbnDRqBP9EL%2BErAlc2UzGa%2B42Ahrc2HlDvyMJCcxLt0Fa2nhXG%0AYWGHsQbHxgbeP" +
                "WHozwun2RXaAvvBonhBaYtcn0QPNEtArB9uyO4YqXXoH1%2Fl0%2Fgh%0AIAzuR%2BLNymwdOBXpyiVFMJb" +
                "6xyQF40aT31kI8Ge%2BUkBbkWDphcEPogd59krBEpwz%0AfBfPdlGoTrSwfbKbshM0kiEbPh%2BESMVvypg" +
                "%2BPZo1Qp0eXYt7gjlYYqNzQHWobTTr%0AIQjY3T8vml7XlcPzxLFqvQliuIZyRLczvm%2BwDhTj%2BJ%2F" +
                "dXAK0SHE%2F9XdqKY014j5t%0AWjUfy9iD6seZ85ntAWdxHmOkytANe3QfyVxbO3N31nFe4uqJmW0RaEDx0" +
                "em3k9YM%0AYLe5OwK%2F49IpUj5gV1R3wnN0uNOZNOdhkyVJynLDJXV5DLoWO3yGMPM3iM%2BZGujk%0A4Q" +
                "rpXjVFscfTHy%2F5%2BbNFGHnapljzli9cbKqt3j610wLQa1pHj6K3xJOANwr0Vdjy%0ABFGwpREQDPceSN" +
                "REFA%2B7FdPh7WQe7P5NbfYuBXGZZeIvRZ6R0EHi8Agxn1426qYJ%0AEJNr%2BqO2r49EhfCdwbizRLhBsq" +
                "MJQIirkf5sI4w5RIgpI9ggkv%2FgQiqxvqFcDdK7%0AAVK%2BeZiB2bvY3SVaH49hWaCE1OZ28gDYPlce6A" +
                "Rxznq1eqQhvgUyOffjpDjPgSkF%0AQhQCj6%2Fle9lunPkNKEYUhFr2eBBabBejRAsdLTOslG3yltICpBjH" +
                "GqOB2CaDlHgL%0Aa5eoeqHusBAx9fmYtd0Zi474cGay8RjGtq%2FE%2B8wDTsupnYGbsHF5pDXC7erW9gyZ" +
                "%0AMzIE8wAZ%2BIxbhG7JXVtHaPWAbvl1ac7YBV7rpBYRKuvZvDQ9BL%2FtYy59HA%3D%3D%0A%3Dt1CY%0" +
                "A-----END%20PGP%20MESSAGE-----&message_id=166b194b21a0997c&senderEmail=human%40flow" +
                "crypt.com&is_outgoing=___cu_false___&account_email=flowcrypt.compatibility%40gmail." +
                "com"
        ),

        "decrypt - [gpg] signed fully armored message" to MessageInfo(
            content = listOf(
                "this was encrypted with gpg",
                "gpg --sign --armor -r flowcrypt.compatibility@gmail.com ./text.txt"
            ),
            quoted = false,
            params = "?frameId=none&message=-----BEGIN%20PGP%20MESSAGE-----%0A%0AowGbwMvMwMVYfy8j1GP" +
                "d8g7GNXlJHCWpFSV6JRUlcSH3akoyMosVyhOLFVLzkosq%0AC0pSUxTKM0syFNIL0rm4gISCrm5xZnoekEo" +
                "sys0vUtAtUkjLyS8Hq9VLzs8tSCzJ%0ATMrMySypdEjPTczMAYkp6OnDrODqZDJmYWDkYpAVU2QJVTh1Tme" +
                "b3HLhpxtYYQ5i%0AZQK5goGLUwAmYl8mwDC3yqJ3RqXeax2n108b42sc%2BI29zE1fLvdgq1Tz3ZL0a2Z5%" +
                "0AXSTDobXyoiGnj748k%2F8iX7dJYc5C%2BTTmPMXtPmYJKmd7V7v2x6675BfR%2Bm25ednr%0APfEB9k%2" +
                "B47iQ9yNsgu9TG8NC%2FhhccalMkT1UUcv7V07mW2ZRbfvSop1ZSU%2FbXm3c%2F%0A8nd%2BZShfmrHQYM" +
                "Mfe3Xmildmbhs2f7S6I8G%2ByamhrH1XsnXKlc%2Fca63S53TU7u5e%0A%2BX7vil97zTc3cDgtP%2Fuw6G" +
                "B6mmTo8mqlb20GytG1LuYzZftP55XYL7XyO5M8Rzx2%0AZcLBPTsfzs8o6bgxt0fBucIlds7nzLOyKld%2B" +
                "G2u%2BuSqzuj9wgpeOSX149f%2B8y7N%2F%0Ahl5nbXIo3qL3QXaWwsXvh7fITVp155%2FbxSXKX65fuLmh" +
                "%2BET24Z9C7V8iGf9M7v76%0AtI%2BjSNRu7cnAttxlX4tOGHhtuMH%2BTU8nNv1cPEc1X%2FH1VRv95mWa" +
                "bl3lP%2BHVmou%2F%0ArkyN1%2FsWl7tS%2FfZP3vVlp3MSPvqy%2FP6T3VKhXSYdWFzhyblB6KhqzAbBuu" +
                "Vf%2F2bY%0AKRx1239v9uZrM3yEZOc0JtzNz7Lh7xb6e89tIne4blx81aRT7b86YroUHGfe0PF4%0AsHjRn" +
                "QWdmeU2kgcmH%2BLUEdxd4bJgx%2FSQwPrb%2B6zieQ0mLbDsvZm7gHFPeq5ZW%2B%2Fe%0ABU8%2FcNc2b" +
                "d49KWrdT8%2FzKpJ9KmvV9uz4AQA%3D%0A%3Dr8So%0A-----END%20PGP%20MESSAGE-----&hasPasswo" +
                "rd=___cu_false___&msgId=1707b9c96c5d7893&senderEmail=flowcrypt.compatibility%40gmai" +
                "l.com&isOutgoing=___cu_true___&acctEmail=flowcrypt.compatibility%40gmail.com"
        ),

        "decrypt - encrypted missing checksum" to MessageInfo(
            content = listOf("400 library systems in 177 countries worldwide"),
            params = "?account_email=flowcrypt.compatibility%40gmail.com&frame_id=frame_DfGthWpEth&m" +
                "essage=-----BEGIN%20PGP%20MESSAGE-----%0AVersion%3A%20FlowCrypt%205.0.4%20Gmail%20E" +
                "ncryption%20flowcrypt.com%0AComment%3A%20Seamlessly%20send%2C%20receive%20and%20sea" +
                "rch%20encrypted%20email%0A%0AwcFMA%2BADv%2F5v4RgKAQ%2F%2BK2rrAqhjMe9FLCfklI9Y30Wokt" +
                "g0Q%2Fxe71EVw6WO%0AtVD%2FVK%2Bxv4CHzi%2BHojtE0U2F%2BvqoPSO0q5TN9giKPMTiK25PnCzfd7Q%" +
                "2BzXiF%0Aj%2B5RSHTVJxC62qLHhtKsAQtC4asub8cQIFXbZz3Ns4%2B7jKtSWPcRqhKTurWv%0AXVH0YAF" +
                "JDsFYo26r2V9c%2BIe0uoQPx8graEGpKO9GtoQjXMKK32oApuBSSlmS%0AQ%2BnxyxMx1V%2BgxP4qgGBCx" +
                "qkBFRYB%2FVe6ygNHL1KxxCVTEw9pgnxJscn89Iio%0AdO6qZ9EgIV0PVQN0Yw033MTgAhCHunlE%2FqXvD" +
                "xib4tdihoNsLN0q5kdOeiMW%0A%2Bntm3kphjMpQ6TMCUGtdS7UmvnadZ%2Bdh5s785M8S9oY64mQd6QuYA" +
                "2iy1IQv%0Aq3zpW4%2Fba2gqL36qCCw%2FOaruXpQ4NeBr3hMaJQjWgeSuMsQnNGYUn5Nn1%2B9X%0Awtli" +
                "thO8eLi3M1dg19dpDky8CacWfGgHD7SNsZ2zqFqyd1qtdFcit5ynQUHS%0AIiJKeUknGv1dQAnPPJ1FdXyy" +
                "qC%2FVDBZG6CNdnxjonmQDRh1YlqNwSnmrR%2FSy%0AX7n%2BnGra%2B%2F0EHJW6ohaSdep2jAwJDelq%2" +
                "FDI1lqiN16ZXJ2%2FWH6pItA9tmkLU%0A61QUz6qwPAnd0t6iy%2FYkOi2%2Fs1%2BdwC0DwOcZoUPF8bTB" +
                "wUwDS1ov%2FOYtlQEB%0AD%2F46rCPRZrX34ipseTkZxtw3YPhbNkNHo95Mzh9lpeaaZIqtUg2yiFUnhwLi" +
                "%0AtYwyBCkXCb92l1GXXxGSmvSLDSKfQfIpZ0rV5j50MYKIpjSeJZyH%2F3qP%2BJXv%0AZ47GsTp0z5%2F" +
                "oNau5XQwuhLhUtRoZd1WS9ahSJ1akiKeYJroLbTg10fjL25yp%0AiaoV16SqKA1H%2FJOuj6lT5z1nuez35" +
                "JjeSpUc7ksdot60ZovMfWC%2BOGRnkYKb%0A7KxFd7uaxL6uOBOFyvRxYeohKd73aVkiKpcWd4orI18Fhlf" +
                "tFNAwIdsmfzNc%0AmzTHZaUl89iYxEKR6ae6AKws1wzLq0noarsf2eKBVbTSfmK3S3xFqduKINnc%0Ae5Yb" +
                "3F5adSj1dUjm1BZ4aqzsgKyBb%2BJ8keG9ESsnFOyxOIUXDM1nIo1IOgzC%0AM928Jb9GVa%2BuhdXRrb5c" +
                "LjTihTusJN0I8oJrwKkwIpCJVgPMdDLkeubrMBQ4%0Afbpl4V76sOU2Nx%2B6nG2FnFBFBFohOL%2B0nTK5" +
                "%2F6Ns9ateN7K9VP%2B%2BQcoeqfPk%0AIUO3%2BlCZW%2BtrTSvvFId3ziUVsPTeuAS%2B7nxSMfWZ%2FK" +
                "9Ci6QV%2FXnx3F%2FqSmuS%0AAUm4zPQ1EjZf1N%2F5K%2BvhcCTN4MMx406VlqtedkXL2KPwZ6jDS%2Fww" +
                "8RfcmPnD%0As94ct0WCZZtNlnQq%2B5h0ybwTJNLC2QFyrhhPqztVY95n9La2Mw5WITCWzg%2Fd%0AIBUce" +
                "W%2FOwHYtePyaSQkCnegDw%2F2mN2%2FGC8d0OlwULcTYG6uVenGv2UOUbCr3%0APfy%2FEb%2FVqUEZK00" +
                "PdvVQV7FWYAshuTFPTqidph04CgQvBpi3SDEEo8SkEIFS%0A%2FiEeRQaWjFEXKUI3FwKXPJQWvFpbrXBOA" +
                "jnxXXbAFYOLxdydmq1GVl9Mm3GU%0AClc9g6t9vaYDBPx2gN562%2FCM%2FnT8Vq45VHe79XkrrcHDwLn7y" +
                "eHJScNFsib%2B%0AVvwTPoUftlhC%2Fai21D403TsJpm7ZmPcDjagoIcXrS%2FlN03z79RBmSKFtYiXW%0A" +
                "4obkKSGow61vMBh2%2FXLVYKJKpYKm%2FGnVlJxA0zQVl558x8I%2FnAMaxSzwx%2BZY%0AwaVU%2Fs5PLZ" +
                "7Ghg3MOguiRTlflKUQyL0A7NR46OjFgUnHAZRxr4KO3GoxVPy4%0AXLeS4%2BWl68s7QlV6WF1IKCHWEUME" +
                "eRRea2%2FOvvlS%2FoLs2MNNWDemlJ4SiXHf%0AxINU38Txo84A00NALbKppsSyy9Gwj%2F%2FrO%2FFcer" +
                "upkfeuOm9nHFwIQeeC5bWD%0AmmRlC90r2jY8gM%2Fv3Jjy9h8PbXWxh9MUpc7%2FkAcTwdGlMxiVjE29p0" +
                "65qTRr%0AOi6sJ7pWuYTfWldZqTVmaBjlv0zuXQ8Eo8o%2FUSvoTs%2BoihYIMcqReqdeqr%2FN%0Ae%2Bs" +
                "DtYKRg%2FLKp%2FJJ5nAQzVMP67DxkgwLNxx0ijBLysaQmvRlsiYWayxZB1Xd%0ABxA2bjZRvsmww%2BhgS" +
                "KNlcsiubJGBqfqvgmlebZuJHHSC1L6mdMYgcihKmYAj%0Ap%2BHFLyqgyeRVMdjRHcrEdxNPG4fJmlk1bYi" +
                "VQQ4XAd72w%2BAHS%2FseZ5HzbAK0%0AomuHYUD5PTEqZ1K9JObSsh3XMUkJK%2Bz3BnrOxnTOOyG2r%2B4" +
                "FxizH6rfz%2FPgg%0AsPxqxE9ELUlgQe8plcPFge6aN9tUoSe%2BvMtDaEAqKw9JwofBF7jlxTqMMvQC%0A" +
                "gWbn9x3W5o4VrnpjYGtPl8sh1QREu0A%2B0PUJAKL4A3GSMYRouGewLSMNJlOg%0A%2F0pPF6qB%2BFi4GJ" +
                "7ju5C07tfr9z9UqRj09kDXJuoJd95NdSiCz6ndugn6gs8B%0AQf%2FXPxZVefeMLiB6p8pG0iZ%2FjcJjyY" +
                "JLtTg6kA%2B1%2FffmJPfH%2F76ZA9dgEJLj%0A%2FW2u0Lp4NY8cwqcXuGKgl72TVJ34Iawl35Y0yr47k%" +
                "2F7Y1vEQ5Q3bT7HP5A%3D%3D%0A-----END%20PGP%20MESSAGE-----&message_id=15f7ffbebc6ba29" +
                "6&senderEmail=flowcrypt.compatibility%40gmail.com&is_outgoing=___cu_true___"
        ),
    )

    private const val messageParam = "message="
  }

  @Test
  fun decryptionTest11() {
    Assert.assertThrows(PgpMsg.NoMdcError::class.java) {
      processMessage(messages["decrypt - [security] mdc - missing - error"]!!)
    }
  }

  @Test
  fun decryptionTest12() {
    Assert.assertThrows(PgpMsg.NoMdcError::class.java) {
      processMessage(messages["decrypt - [security] mdc - modification detected - error"]!!)
    }
  }

  @Test
  fun decryptionTest13() {
    Assert.assertThrows("Primary user is revoked", PgpMsg.OtherError::class.java) {
      processMessage(messages["decrypt - [everdesk] message encrypted for sub but claims " +
          "encryptedFor:primary,sub"]!!)
    }
  }

  @Test
  fun decryptionTest14() {
    // ??? must be successful ???
    Assert.assertThrows(PgpMsg.OtherError::class.java) {
      processMessage(messages["decrypt - [gpg] signed fully armored message"]!!)
    }
  }

  @Test
  fun decryptionTest15() {
    Assert.assertThrows(PgpMsg.NoMdcError::class.java) {
      processMessage(messages["decrypt - encrypted missing checksum"]!!)
    }
  }

  @Test
  fun decryptionTest1() {
    val messageInfo = messages["decrypt - without a subject"]!!
    val text = processMessage(messageInfo)
    assertEquals(messageInfo.content, text)
  }

  private fun processMessage(messageInfo: MessageInfo): String {
    val urlEncodedText = messageInfo.params.split('&').filter { it.startsWith(messageParam) }
        .map { it.substring(messageParam.length) }.first()
    val text = URLDecoder.decode(urlEncodedText, "UTF-8")
    val result = PgpMsg.decrypt(privateKeys, text.toByteArray(), null)
    val s = String(result.content)
    println(s)
    return s
  }
}
