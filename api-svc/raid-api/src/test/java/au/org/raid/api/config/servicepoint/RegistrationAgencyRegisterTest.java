package au.org.raid.api.config.servicepoint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationAgencyRegisterTest {
    private static final String ARDC_ROR = "https://ror.org/038sjwq14";
    private static final String SDSC_ROR = "https://ror.org/04mg3nk07";

    private static Resource register(final String yaml) {
        return new ByteArrayResource(yaml.getBytes());
    }

    private static RegistrationAgencyRegister shipped() {
        return new RegistrationAgencyRegister(
                new ClassPathResource(RegistrationAgencyRegister.DEFAULT_RESOURCE_PATH));
    }

    @Test
    @DisplayName("derives a block's first Service Point ID from its index")
    void derivesStartFromBlock() {
        final var subject = shipped();

        assertThat(subject.servicePointIdStart(ARDC_ROR)).isEqualTo(20000000L);
        assertThat(subject.servicePointIdStart(SDSC_ROR)).isEqualTo(40000000L);
    }

    @Test
    @DisplayName("scales past a single leading digit")
    void scalesPastOneDigit() {
        final var subject = new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: Fortieth agency
                    ror: https://ror.org/example40
                    block: 40
                """));

        assertThat(subject.servicePointIdStart("https://ror.org/example40")).isEqualTo(400000000L);
    }

    @Test
    @DisplayName("refuses to start when no agency identifier is configured")
    void rejectsMissingIdentifier() {
        final var subject = shipped();

        assertThatThrownBy(() -> subject.servicePointIdStart(null))
                .isInstanceOf(RegistrationAgencyNotRegisteredException.class)
                .hasMessageContaining("raid.identifier.registration-agency-identifier is not set");

        assertThatThrownBy(() -> subject.servicePointIdStart("  "))
                .isInstanceOf(RegistrationAgencyNotRegisteredException.class)
                .hasMessageContaining("raid.identifier.registration-agency-identifier is not set");
    }

    @Test
    @DisplayName("refuses to start for an agency with no allocation, and says where to get one")
    void rejectsUnregisteredAgency() {
        final var subject = shipped();

        assertThatThrownBy(() -> subject.servicePointIdStart("https://ror.org/notallocated"))
                .isInstanceOf(RegistrationAgencyNotRegisteredException.class)
                .hasMessageContaining("https://ror.org/notallocated")
                .hasMessageContaining("RAiD Registration Authority");
    }

    @Test
    @DisplayName("never resolves a reserved block to an agency")
    void reservedBlockIsNotResolvable() {
        final var subject = new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: Local development and test
                    block: 1
                    reserved: true
                  - name: An agency
                    ror: https://ror.org/example
                    block: 2
                """));

        assertThat(subject.find("https://ror.org/example")).isPresent();
        assertThat(subject.agencies()).hasSize(2);
        assertThat(subject.agencies())
                .filteredOn(RegistrationAgency::reserved)
                .singleElement()
                .satisfies(reserved -> assertThat(reserved.ror()).isNull());
    }

    @Test
    @DisplayName("rejects a block allocated twice")
    void rejectsDuplicateBlock() {
        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: First
                    ror: https://ror.org/first
                    block: 2
                  - name: Second
                    ror: https://ror.org/second
                    block: 2
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("allocated more than once");
    }

    @Test
    @DisplayName("rejects a ROR listed twice")
    void rejectsDuplicateRor() {
        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: First
                    ror: https://ror.org/same
                    block: 2
                  - name: Second
                    ror: https://ror.org/same
                    block: 3
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("appears more than once");
    }

    @Test
    @DisplayName("rejects an agency with no ROR or a malformed one")
    void rejectsBadRor() {
        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: No ROR
                    block: 2
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must have a ROR");

        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: Not a ROR
                    ror: 038sjwq14
                    block: 2
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must have a ROR");
    }

    @Test
    @DisplayName("rejects a reserved block carrying a ROR")
    void rejectsReservedWithRor() {
        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: Reserved
                    ror: https://ror.org/example
                    block: 1
                    reserved: true
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not carry a ROR");
    }

    @Test
    @DisplayName("rejects an entry with no name or no positive block")
    void rejectsIncompleteEntry() {
        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - ror: https://ror.org/example
                    block: 2
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no name");

        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                blockSize: 10000000
                agencies:
                  - name: No block
                    ror: https://ror.org/example
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no positive block");
    }

    @Test
    @DisplayName("rejects a register with no agencies or no blockSize")
    void rejectsEmptyRegister() {
        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("blockSize: 10000000\n")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lists no agencies");

        assertThatThrownBy(() -> new RegistrationAgencyRegister(register("""
                agencies:
                  - name: An agency
                    ror: https://ror.org/example
                    block: 2
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no blockSize");
    }

    /**
     * The gate on any future onboarding pull request. A duplicated block or ROR
     * would reintroduce the collision the register exists to prevent, so the
     * shipped file must satisfy every invariant at build time.
     */
    @Test
    @DisplayName("the shipped register is internally consistent")
    void shippedRegisterIsValid() {
        final var subject = shipped();

        assertThat(subject.blockSize()).isEqualTo(10000000L);
        assertThat(subject.agencies()).isNotEmpty();
        assertThat(subject.find(ARDC_ROR))
                .get()
                .satisfies(ardc -> assertThat(ardc.block()).isEqualTo(2));
        assertThat(subject.agencies())
                .filteredOn(agency -> agency.block() == 1)
                .singleElement()
                .satisfies(local -> assertThat(local.reserved()).isTrue());
    }
}
