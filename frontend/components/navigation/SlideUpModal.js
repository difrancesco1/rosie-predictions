import React, { useEffect, useRef } from 'react';
import styles from '../../styles/SlideUpModal.module.css';

const SlideUpModal = ({ isOpen, onClose, children, title }) => {
    const modalRef = useRef(null);

    useEffect(() => {
        // Add animation classes after component mounts
        if (isOpen && modalRef.current) {
            modalRef.current.classList.add(styles.visible);
        }
    }, [isOpen]);

    const handleClose = () => {
        if (modalRef.current) {
            // Remove the visible class to start the exit animation
            modalRef.current.classList.remove(styles.visible);

            // Wait for animation to complete before actually closing
            setTimeout(() => {
                onClose();
            }, 300); // Match this to your animation duration
        }
    };

    if (!isOpen) return null;

    return (
        <div className={styles.modalOverlay}>
            <div className={styles.modalContainer} ref={modalRef}>
                <div className={styles.modalHeader}>
                    <h3 className={styles.modalTitle}>{title}</h3>
                    <button className={styles.closeButton} onClick={handleClose}>
                        ×
                    </button>
                </div>
                <div className={styles.modalContent}>
                    {children}
                </div>
            </div>
        </div>
    );
};

export default SlideUpModal;