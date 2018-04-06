package uk.gov.hmcts.reform.sendletter.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;
import java.util.Optional;

import static org.bouncycastle.openpgp.PGPUtil.getDecoderStream;

public final class PgpEncryptionUtil {

    // Prevent instantiation.
    private PgpEncryptionUtil() {
    }

    public static byte[] encryptFile(
        byte[] inputFile,
        String fileNamePrefix,
        String fileNameSuffix,
        PGPPublicKey pgpPublicKey,
        boolean withIntegrityCheck
    ) throws IOException, PGPException {
        Security.addProvider(new BouncyCastleProvider());

        ByteArrayOutputStream byteArrayOutputStream =
            compressAndWriteFileToLiteralData(
                inputFile,
                fileNamePrefix,
                fileNameSuffix
            );

        PGPEncryptedDataGenerator encryptedDataGenerator =
            prepareDataEncryptor(
                pgpPublicKey,
                withIntegrityCheck
            );

        return writeEncryptedDataToOutputStream(byteArrayOutputStream, encryptedDataGenerator);
    }

    /**
     * Returns raw key bytes as a Bouncy Castle PGP public key.
     */
    public static PGPPublicKey loadPublicKey(byte[] data) throws IOException {
        return lookupPublicSubkey(
            new BcPGPPublicKeyRing(
                getDecoderStream(new ByteArrayInputStream(data))
            )
        ).orElseThrow(() -> new UnableToLoadPgpPublicKeyException("PGP Public key object could be constructed"));
    }

    /**
     * Return appropriate key or subkey for given task from public key.
     * Weirder older PGP public keys will actually have multiple keys. The main key will usually
     * be sign-only in such situations. So you've gotta go digging in through the key packets and
     * make sure you get the one that's valid for encryption.
     */
    public static Optional<PGPPublicKey> lookupPublicSubkey(PGPPublicKeyRing ring) {
        Iterator<PGPPublicKey> keys = ring.getPublicKeys();
        while (keys.hasNext()) {
            PGPPublicKey key = keys.next();
            if (key.isEncryptionKey()) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }

    private static byte[] writeEncryptedDataToOutputStream(
        ByteArrayOutputStream bout,
        PGPEncryptedDataGenerator encryptedDataGenerator
    ) throws IOException, PGPException {
        byte[] bytes = bout.toByteArray();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        OutputStream outputStream = encryptedDataGenerator.open(byteArrayOutputStream, bytes.length);
        outputStream.write(bytes);
        outputStream.close();

        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    private static PGPEncryptedDataGenerator prepareDataEncryptor(
        PGPPublicKey pgpPublicKey,
        boolean withIntegrityCheck
    ) {
        BcPGPDataEncryptorBuilder dataEncryptor = new BcPGPDataEncryptorBuilder(PGPEncryptedData.TRIPLE_DES);
        dataEncryptor.setWithIntegrityPacket(withIntegrityCheck);
        dataEncryptor.setSecureRandom(new SecureRandom());

        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(dataEncryptor);
        encryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(pgpPublicKey));
        return encryptedDataGenerator;
    }

    private static ByteArrayOutputStream compressAndWriteFileToLiteralData(
        byte[] inputFile,
        String fileNamePrefix,
        String fileNameSuffix
    ) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        PGPCompressedDataGenerator pgpCompressedDataGenerator =
            new PGPCompressedDataGenerator(PGPCompressedData.ZIP);

        //Creates an empty file in the default temporary-file directory
        File tempFile = createTempFile(inputFile, fileNamePrefix, fileNameSuffix);

        PGPUtil.writeFileToLiteralData(
            pgpCompressedDataGenerator.open(byteArrayOutputStream),
            PGPLiteralData.BINARY,
            tempFile
        );

        pgpCompressedDataGenerator.close();

        return byteArrayOutputStream;
    }

    private static File createTempFile(
        byte[] inputFile,
        String fileNamePrefix,
        String fileNameSuffix
    ) throws IOException {
        File tempFile = File.createTempFile(fileNamePrefix, fileNameSuffix);
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(inputFile);
        return tempFile;
    }
}
