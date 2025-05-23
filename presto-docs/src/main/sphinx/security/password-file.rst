.. _password_file_auth:

============================
Password File Authentication
============================

Presto can be configured to enable frontend password authentication over
HTTPS for clients, such as the CLI, or the JDBC and ODBC drivers. The
username and password are validated against usernames and passwords stored
in a file.

Password file authentication is very similar to :doc:`ldap`. Please see
the LDAP documentation for generic instructions on configuring the server
and clients to use TLS and authenticate with a username and password.

Password Authenticator Configuration
------------------------------------

Enable password file authentication by creating an
``etc/password-authenticator.properties`` file on the coordinator:

.. code-block:: none

    password-authenticator.name=file
    file.password-file=/path/to/password.db

The following configuration properties are available:

==================================== ==============================================
Property                             Description
==================================== ==============================================
``file.password-file``               Path of the password file.

``file.refresh-period``              How often to reload the password file.
                                     Defaults to ``5s``.

``file.auth-token-cache.max-size``   Max number of cached authenticated passwords.
                                     Defaults to ``1000``.
==================================== ==============================================

Password Validation
-------------------

Password validation in Presto supports both `PBKDF2WithHmacSHA256` and `PBKDF2WithHmacSHA1` algorithms. 
To ensure modern cryptographic standards, clients are encouraged to use `PBKDF2WithHmacSHA256`. 
A fallback mechanism is available to maintain compatibility with legacy systems using `PBKDF2WithHmacSHA1`.

Migration to `PBKDF2WithHmacSHA256` is strongly recommended to maintain security.

API Method
^^^^^^^^^^
The following method uses the `PBKDF2WithHmacSHA256` validation mechanism and includes a fallback mechanism:

.. code-block:: java

    /**
     * @Deprecated using PBKDF2WithHmacSHA1 is deprecated and clients should switch to PBKDF2WithHmacSHA256
     */
    public static boolean doesPBKDF2PasswordMatch(String inputPassword, String hashedPassword)
    {
        PBKDF2Password password = PBKDF2Password.fromString(hashedPassword);

        // Validate using PBKDF2WithHmacSHA256
        if (validatePBKDF2Password(inputPassword, password, "PBKDF2WithHmacSHA256")) {
            return true;
        }

        // Fallback to PBKDF2WithHmacSHA1
        LOG.warn("Using deprecated PBKDF2WithHmacSHA1 for password validation.");
        return validatePBKDF2Password(inputPassword, password, "PBKDF2WithHmacSHA1");
    }

**Fallback Mechanism**

If `PBKDF2WithHmacSHA256` fails for legacy reasons, the system gracefully falls back to `PBKDF2WithHmacSHA1` while logging a warning.

Password Files
--------------

File Format
^^^^^^^^^^^

The password file contains a list of usernames and passwords, one per line,
separated by a colon. Passwords must be securely hashed using bcrypt or PBKDF2.

bcrypt passwords start with ``$2y$`` and must use a minimum cost of ``8``:

.. code-block:: none

    test:$2y$10$BqTb8hScP5DfcpmHo5PeyugxHz5Ky/qf3wrpD7SNm8sWuA3VlGqsa

PBKDF2 passwords are composed of the iteration count, followed by the
hex encoded salt and hash:

.. code-block:: none

    test:1000:5b4240333032306164:f38d165fce8ce42f59d366139ef5d9e1ca1247f0e06e503ee1a611dd9ec40876bb5edb8409f5abe5504aab6628e70cfb3d3a18e99d70357d295002c3d0a308a0

Creating a Password File
^^^^^^^^^^^^^^^^^^^^^^^^

Password files utilizing the bcrypt format can be created using the
`htpasswd <https://httpd.apache.org/docs/current/programs/htpasswd.html>`_
utility from the `Apache HTTP Server <https://httpd.apache.org/>`_.
The cost must be specified, as Presto enforces a higher minimum cost
than the default.

Create an empty password file to get started:

.. code-block:: none

    touch password.db

Add or update the password for the user ``test``:

.. code-block:: none

    htpasswd -B -C 10 password.db test

